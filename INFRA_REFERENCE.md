# Infra Reference

CI/CD 파이프라인과 배포 인프라의 동작 원리를 정리한 문서입니다.
EC2 IP, 인증 정보 등 민감한 값은 여기 적지 않습니다 — 팀 Notion(비공개)을 참고하세요.

## 인프라 구성

```
GitHub Actions ── build & push ──▶ Docker Hub ── pull ──▶ EC2 (Docker 컨테이너)
```

- **CI/CD**: GitHub Actions
- **이미지 레지스트리**: Docker Hub
- **배포 대상**: AWS EC2, Docker 컨테이너 직접 실행 (오케스트레이션 도구 없음)
- **API 컨테이너**: `promsearch`, 포트 `8080:8080`
- **이미지 Worker 컨테이너**: `promsearch-worker`, HTTP 포트 없음

## 워크플로우 구성

레포에는 워크플로우 2개가 있고, 서로 `workflow_run`으로 연결되어 있습니다.

### `ci.yml` — 검증

- 트리거: `develop`, `main` 대상 `push` / `pull_request`
- `feature/*` → `develop` PR, `develop` → `main` PR 모두 여기서 빌드 + 테스트(`./gradlew build`)와 Docker 이미지 빌드를 수행합니다.
- 이 워크플로우는 배포를 하지 않습니다. 실패해도 재시도가 자유로워야 하므로 알림도 없습니다.
- 동일 브랜치의 이전 CI는 취소하고 최신 커밋만 검증하며, Gradle 의존성 캐시를 사용합니다.

### `deploy.yml` — 배포

- 트리거: `workflow_run` (`ci.yml`의 실행 완료 이벤트), `branches: [main]`으로 제한
- CI 실행 이벤트가 `push`이고, 대상 브랜치가 `main`이며, 실행 결과가 `success`일 때만 실제로 동작합니다.
  - `develop`에서 CI가 성공해도 이 조건에서 걸러져 배포로 이어지지 않습니다.
  - `main`에서 CI가 실패하면 이 워크플로우 자체는 실행되지만 `build-and-push` job이 스킵됩니다.
- 체크아웃 시 `ref: ${{ github.event.workflow_run.head_sha }}`를 사용합니다. `main`의 최신 tip이 아니라 **CI가 실제로 검증한 커밋**을 그대로 배포하기 위함입니다 (CI 종료 시점과 배포 시작 시점 사이에 `main`이 더 앞서가는 레이스 컨디션을 방지).
- 이미지는 `latest`와 커밋 SHA 두 개 태그로 Docker Hub에 push됩니다. SHA 태그는 롤백 시 특정 커밋의 이미지를 지정해서 재배포할 수 있게 해줍니다.
- EC2에서는 SSH로 접속해 기존 API/Worker 컨테이너를 내리고 같은 이미지로 재기동합니다.
  - API는 기본 `api.jar`, Worker는 명시한 `worker.jar`를 실행합니다.
  - CI가 검증한 커밋 SHA 이미지로 API를 기동하고, API 헬스체크가 완료된 뒤 Worker를 시작합니다.
  - Worker의 첫 SQS Long Polling 성공 로그까지 확인한 뒤 배포를 완료합니다.
  - API 또는 Worker 검증이 실패하면 직전 컨테이너를 복구합니다.
  - Docker 명령과 로그 조회에는 제한 시간을 두며, Worker readiness는 최근 200줄만 확인합니다.
  - API와 Worker 로그는 컨테이너별 최대 10MB × 3파일로 순환합니다.
  - API가 Flyway를 실행하며, 새 빈 PostgreSQL에는 V1 전체 초기 스키마를 적용합니다.
  - `--restart unless-stopped`: EC2가 재부팅되어도 컨테이너가 자동으로 다시 뜹니다.
  - `docker image prune -f`: 배포마다 쌓이는 이전 이미지를 정리합니다.
- 배포(`deploy` job) 실패 시 Discord 웹훅으로 알림이 갑니다.
- GitHub `production` Environment를 사용하므로 필요하면 Environment protection rule에서 배포 승인을 설정할 수 있습니다.

## 필요한 GitHub Secrets

값은 저장소 Settings → Secrets and variables → Actions에 등록되어 있으며, 이 문서에는 이름만 기록합니다.

| Secret | 용도 |
| --- | --- |
| `DOCKERHUB_USERNAME` | Docker Hub 로그인 및 이미지 이름(`{username}/promsearch`) |
| `DOCKERHUB_TOKEN` | Docker Hub 로그인 토큰 |
| `EC2_HOST` | 배포 대상 EC2 퍼블릭 IP/도메인 |
| `EC2_USERNAME` | EC2 SSH 접속 계정 |
| `EC2_SSH_KEY` | EC2 SSH 접속 프라이빗 키 |
| `DISCORD_WEBHOOK` | 배포 실패 알림용 Discord 웹훅 URL |
| `JWT_ACCESS_SECRET` | Access Token 서명 키 (HS256, 32바이트 이상) |
| `JWT_REFRESH_SECRET` | Refresh Token 서명 키 (HS256, 32바이트 이상) |
| `SWAGGER_ENABLE` | prod에서 Swagger 문서 노출 여부 (보통 `false`) |
| `SWAGGER_AUTH_USERNAME` | `SWAGGER_ENABLE=true`일 때 Swagger Basic Auth 계정 |
| `SWAGGER_AUTH_PASSWORD` | `SWAGGER_ENABLE=true`일 때 Swagger Basic Auth 비밀번호 |
| `KAKAO_CLIENT_ID` | 카카오 소셜 로그인 REST API 키 |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret 사용 시에만 필요 (아니면 빈 값) |
| `GOOGLE_CLIENT_ID` | 구글 소셜 로그인 OAuth 2.0 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | 구글 소셜 로그인 OAuth 2.0 클라이언트 Secret |
| `SPRING_DATASOURCE_URL` | API와 Worker가 공유하는 PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL 사용자 이름 |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL 비밀번호 |
| `SPRING_FLYWAY_ENABLED` | 새 빈 운영 DB는 `true` |
| `AWS_S3_BUCKET` | 원본·워터마크 이미지를 저장할 S3 버킷 |
| `AWS_SQS_WATERMARK_ENABLED` | SQS 발행기와 Worker 활성화 여부 (`true`) |
| `AWS_SQS_WATERMARK_QUEUE_URL` | 이미지 워터마크 작업 SQS Queue URL |

`deploy.yml`은 위 값들을 `docker run -e`로 컨테이너에 직접 전달합니다 (`docker run`에 아무 환경변수도 넘기지 않던 이전 버전에서는 컨테이너가 필수 설정값 검증에서 부팅에 실패했습니다).
Worker 배포는 `AWS_SQS_WATERMARK_ENABLED=true`와 비어 있지 않은 Queue URL을 요구합니다.

## 브랜치 ↔ 파이프라인 매핑

자세한 브랜치 컨벤션은 [CONTRIBUTING.md](.github/CONTRIBUTING.md) 참고. 여기서는 파이프라인 동작만 정리합니다.

| 브랜치 | CI | 배포 |
| --- | --- | --- |
| `feature/*`, `fix/*` 등 작업 브랜치 | PR 생성 시 실행 (base: `develop`) | 없음 |
| `develop` | push/PR 시 실행 | 없음 (검증 전용) |
| `main` | push/PR 시 실행 | CI 성공 시 자동 배포 |

## 로컬에서 배포 파이프라인 확인하기

```bash
# Dockerfile 기준으로 이미지가 정상 빌드되는지 확인
docker build -t promsearch .
docker run -p 8080:8080 promsearch
docker run --no-healthcheck promsearch worker.jar
```

Actions 탭에서 워크플로우 로그를 직접 볼 수 없는 상황이라면, 위 명령으로 최소한 이미지 빌드 자체가 깨지지 않는지 먼저 확인합니다.

## 트러블슈팅

- **`main`에 push했는데 Deploy가 안 도는 경우**: Actions 탭에서 `CI` 워크플로우 실행이 `main`을 대상으로 성공(`success`)했는지 먼저 확인합니다. `develop`에서의 성공은 배포를 트리거하지 않습니다.
- **Deploy는 실행됐는데 `build-and-push`가 스킵된 경우**: 연결된 CI 실행의 `conclusion`이 `success`가 아니었다는 뜻입니다. CI 로그를 확인하세요.
- **EC2 배포 스텝에서 SSH 연결 실패**: `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY` secret 값과 EC2 보안 그룹의 인바운드 규칙(22번 포트)을 확인합니다.
- **Docker Hub push 실패**: `DOCKERHUB_TOKEN`이 만료되었거나 권한이 부족한 경우가 많습니다. Docker Hub Access Token을 재발급하세요.
