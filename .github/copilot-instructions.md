# PromSearch - AI Agent Guidelines

이 문서는 PromSearch 저장소에서 AI Agent가 코드를 작성하거나 리뷰할 때 따를 기준입니다.
답변과 리뷰 코멘트는 특별한 요청이 없으면 한국어로 작성합니다.

## 프로젝트 개요

PromSearch는 실용적인 AI 프롬프트를 공유, 탐색, 언락할 수 있는 프롬프트 마켓플레이스/커뮤니티입니다.
MVP는 실제 결제 대신 포인트 시스템, 유료 콘텐츠 마스킹, 언락 기록, 창작자 지표를 통해 수익화 가능성을 검증합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5
- Gradle
- JUnit 5

## 권장 도메인

```text
promsearch
├── auth
├── user
├── prompt
├── point
├── interaction
├── comment
├── moderation
├── tracking
├── admin
└── global
```

각 주요 도메인은 다음 계층을 기준으로 구성합니다.

```text
{domain}
├── interfaces
│   ├── {Domain}Controller.java
│   └── dto
├── application
│   ├── {Action}{Domain}UseCase.java
│   ├── {Domain}Service.java
│   └── port
│       └── out
├── domain
└── infrastructure
    └── persistence
```

## 아키텍처 규칙

- 비즈니스 규칙은 `domain`에 둡니다.
- Controller는 비즈니스 로직을 갖지 않습니다.
- Controller는 Repository가 아니라 UseCase를 호출합니다.
- Application Service는 트랜잭션 경계와 유스케이스 오케스트레이션을 담당합니다.
- Domain은 Spring, JPA, Application, Infrastructure에 의존하지 않습니다.
- Application은 Domain과 `application.port.out`에 의존합니다.
- Infrastructure는 `application.port.out`을 구현합니다.
- Spring Data JPA Repository를 Application Service에 직접 주입하지 않습니다.
- Controller에서 JPA Entity를 직접 반환하지 않습니다.
- Request DTO는 Command/Query 객체로 변환한 뒤 UseCase에 전달합니다.
- Response DTO는 Application Result/Info 객체를 기반으로 생성합니다.
- 다른 도메인 Aggregate는 객체 참조 대신 ID로 참조합니다.

## 도메인 모델 규칙

- 도메인 객체 생성은 정적 팩터리 메서드를 사용합니다.
- 직접 생성자는 `private` 또는 `protected`로 제한합니다.
- Domain Entity에 Setter를 만들지 않습니다.
- 상태 변경은 명시적인 도메인 메서드로 표현합니다.

권장 팩터리 이름:

- `create()`: 새 도메인 객체 생성
- `of()`: 여러 primitive 또는 값으로 생성
- `from()`: 다른 객체 하나에서 변환
- `reconstruct()`: DB에서 읽은 상태로 복원

## PromSearch 핵심 정책

- `FREE` 프롬프트는 본문 전체를 즉시 반환할 수 있습니다.
- `PREMIUM`, `MASTER` 프롬프트는 언락 전 원문을 절대 반환하지 않습니다.
- 유료 콘텐츠 마스킹은 백엔드에서 보장해야 합니다.
- 프론트엔드는 언락 권한이 없는 유료 프롬프트 원문을 받을 수 없어야 합니다.
- 포인트 차감과 언락 기록은 일관성을 갖도록 하나의 트랜잭션에서 처리합니다.
- 조회수, 복사, 좋아요, 신고, 댓글 등 지표는 도메인 정책과 집계 정책을 분리합니다.

## 테스트 기준

- UseCase 단위 테스트를 우선 작성합니다.
- 포인트 차감, 언락 중복 방지, 유료 콘텐츠 마스킹은 반드시 테스트합니다.
- Controller 테스트에서는 민감 필드가 응답에 포함되지 않는지 확인합니다.
- 테스트 이름은 의도가 드러나게 작성합니다.

## Git / PR 규칙

커밋 메시지는 Conventional Commits를 사용합니다.

```text
feat: add prompt unlock usecase
fix: prevent paid prompt body exposure
```

PR 제목은 대괄호 태그를 사용합니다.

```text
[Feat] 프롬프트 언락 기능 추가
[Fix] 유료 프롬프트 원문 노출 방지
```
