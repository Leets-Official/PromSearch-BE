# Contributing Guidelines

## 개요

PromSearch는 프롬프트 마켓플레이스/커뮤니티 서비스의 API 서버입니다.
이 저장소의 Issue와 Pull Request는 팀 내부 개발, 리뷰, 배포 관리를 위해 사용합니다.

## 브랜치 전략

- 기본 브랜치: `main`
- 기능 브랜치: `feat/{issue-number}-{short-description}`
- 버그 수정 브랜치: `fix/{issue-number}-{short-description}`
- 문서/설정 브랜치: `docs/{issue-number}-{short-description}`, `chore/{issue-number}-{short-description}`

팀에서 `develop` 브랜치를 운영하기로 결정하면 PR 대상 브랜치를 `develop`으로 맞춰주세요.

## 커밋 메시지

Conventional Commits 형식을 사용합니다.

```text
<type>: <subject>
```

사용 가능한 type:

- `feat`: 기능 추가
- `fix`: 버그 수정
- `refactor`: 기능 변경 없는 구조 개선
- `docs`: 문서 작성/수정
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, CI/CD 등 기타 작업

예시:

```text
feat: add prompt unlock usecase
fix: mask paid prompt body before unlock
```

## Pull Request 규칙

- PR 제목은 `[Feat] 프롬프트 언락 기능 추가`처럼 대괄호 태그를 사용합니다.
- PR 본문에 연관 이슈, 작업 내용, 테스트 결과를 작성합니다.
- 유료 프롬프트 원문, 토큰, 개인정보가 로그나 응답 예시에 포함되지 않도록 주의합니다.
- 최소 1명 이상의 리뷰 승인 후 병합합니다.

## 아키텍처 원칙

- 패키지는 도메인 기준으로 나눕니다. 예: `auth`, `user`, `prompt`, `commerce`, `community`, `moderation`, `tracking`, `admin`, `common`, `global`
- Controller는 비즈니스 로직을 갖지 않고 UseCase를 호출합니다.
- Application Service는 트랜잭션 경계와 유스케이스 오케스트레이션을 담당합니다.
- Domain은 Spring, JPA, Application, Infrastructure에 의존하지 않습니다.
- Infrastructure는 JPA, QueryDSL, 외부 API, 파일 저장소, Redis 등 구현 세부사항을 담당합니다.
- 다른 도메인 Aggregate는 객체 참조 대신 ID로 참조합니다.
- JPA Entity를 Controller 응답으로 직접 반환하지 않습니다.

## 로컬 검증

PR 생성 전 아래 명령 중 하나 이상을 실행해주세요.

```bash
./gradlew test
./gradlew build
```
