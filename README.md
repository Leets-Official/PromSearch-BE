# PromSearch Backend

프롬써치(PromSearch) MVP 백엔드 API 서버입니다. 아웃풋(결과물) 중심 프롬프트 탐색 → 로그인/가입 → 상세 조회 → 복사/추천/업로드 → 집계/등급/운영 관리 흐름을 지원합니다.

## 기술 스택

- **언어**: Java 21 (LTS)
- **프레임워크**: Spring Boot 3.5.16
- **빌드 도구**: Gradle
- **컨테이너**: Docker (멀티스테이지 빌드, `eclipse-temurin:21`)
- **배포**: AWS EC2 (Docker 컨테이너 직접 배포)
- **이미지 레지스트리**: Docker Hub
- **CI/CD**: GitHub Actions

## 로컬 실행 방법

```bash
./gradlew bootRun
```

정상 실행되면 아래 주소로 헬스체크 확인:
```
http://localhost:8080/test/health-check
→ OK
```

## Docker로 로컬 실행

```bash
docker build -t promsearch .
docker run -p 8080:8080 promsearch
```

## 배포된 서버 확인

```
http://[EC2_퍼블릭_IP]:8080/test/health-check
```

실제 IP는 팀 내부 인프라 문서(Notion) 참고. 이 값은 보안상 README에 직접 기재하지 않습니다.

## 브랜치 전략

```
main            → 배포 브랜치 (protected, 직접 push 불가)
feature/기능명   → 작업 브랜치, 예: feature/auth-login, feature/prompt-upload
```

- `main`으로 머지하려면:
  1. `feature/*` 브랜치에서 작업 후 PR 생성
  2. 팀원 1명 이상 승인(Approve)
  3. CI(`build-and-test`) 통과
  - 위 조건 미충족 시 GitHub이 머지 버튼을 자동으로 막습니다.
- `main`에 직접 push 및 force push는 차단되어 있습니다.

## CI/CD 동작 방식

```
[feature 브랜치 push / PR] → CI 실행 (빌드 + 테스트)
                                └ 실패해도 알림 없음, 개발 중 자유롭게 반복 가능

[PR 승인 + main 머지] → CI 재실행 → 성공 시에만 CD 실행
                          └ Docker 이미지 빌드 → Docker Hub 푸시 (latest + git sha 태그)
                          → EC2 SSH 접속 → 컨테이너 재기동
                          └ 실패 시 Discord #ci-cd 채널 알림
```

- CI가 실패한 커밋은 **절대 배포되지 않습니다** (`workflow_run` + `conclusion == 'success'` 조건으로 연결).
- 배포된 컨테이너는 `--restart unless-stopped`로 EC2 재부팅 시에도 자동 기동됩니다.
- 배포 시마다 이전 이미지는 `docker image prune -f`로 정리됩니다.

## 커밋 전 체크리스트

- [ ] `./gradlew build` 로컬에서 통과 확인 (CI 실패 최소화)
- [ ] 커밋 메시지 컨벤션 확인 *(팀 컨벤션 문서 링크 추가 예정)*
- [ ] 민감정보(API 키, 비밀번호 등)를 코드/설정 파일에 하드코딩하지 않았는지 확인

## 프로젝트 문서

- API 명세서 / ERD: *(링크 추가 예정)*
- 팀 개발 컨벤션: *(링크 추가 예정)*
- 인프라/인증정보 레퍼런스: 팀 Notion (비공개) — GitHub에는 올리지 않습니다.
