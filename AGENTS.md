# PromSearch Backend - Agent Guidelines

This document provides repository-specific instructions for AI agents working in the PromSearch backend. These instructions take precedence over general workflows. Respond in Korean unless code, package names, or technical identifiers require English.

## Role

Act as a senior Java/Spring collaborator. Optimize for reliable paid prompt access, point/unlock consistency, clear domain boundaries, and maintainable Clean/Hexagonal Architecture.

## Tech Stack

- Java 21
- Spring Boot 3.5
- Gradle
- JPA/Hibernate
- JUnit 5
- Flyway recommended for migrations

## Domain Structure

```text
com.promsearch
├── auth
├── user
├── prompt
├── commerce
├── community
├── moderation
├── tracking
├── admin
├── common
└── global
```

Domain responsibilities:

- `auth`: 로그인, 로그아웃, 토큰 재발급 등 인증 처리
- `user`: 회원 정보, 프로필, 권한, 크리에이터 등급, 유저 상태 관리
- `prompt`: 프롬프트 콘텐츠 본체, 이미지, 태그, 검색, 통계 관리
- `commerce`: 포인트 내역, 유료 프롬프트 언락, 창작자 보상 처리
- `community`: 댓글, 좋아요, 북마크 등 프롬프트에 대한 사용자 반응
- `moderation`: 신고, 자동 숨김, 게시글/댓글 숨김 및 해제, 검토 정책
- `tracking`: 카드 클릭, 상세 진입, 복사 클릭 등 이벤트 로그 수집
- `admin`: 관리자 API 입구. 실제 처리는 각 도메인의 UseCase를 호출
- `common`: 여러 계층에서 함께 쓰는 공통 기반 클래스와 공통 예외
- `global`: 전역 설정, 전역 예외 처리, 공통 응답, 보안, 유틸

## Layer Structure

Each main domain should use this internal structure as it grows:

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

Layer responsibilities:

- `interfaces`: Controller, request/response DTO, API input/output conversion
- `application`: UseCase, service, transaction boundary, orchestration, authorization flow
- `application.port.out`: output ports hiding repositories/external systems from application services
- `domain`: entity, value object, enum, domain service, business rules
- `domain.enums`: domain enum values
- `infrastructure`: JPA entity, Spring Data repository, QueryDSL, external API, file storage, Redis

## Dependency Rules

- `interfaces` may depend on `application`.
- `application` may depend on `domain` and `application.port.out`.
- `infrastructure` may depend on `domain` and implement `application.port.out`.
- `domain` must not depend on Spring, JPA, `application`, `interfaces`, or `infrastructure`.
- `application` must not depend on controllers, request DTOs, or Spring Data repositories.
- Controllers must call UseCases, not repositories.
- Do not return JPA entities from controllers.
- Convert request DTOs to Command/Query objects before calling UseCases.
- Create response DTOs from application Result/Info objects.
- Reference other domain aggregates by ID, not direct object references.

## Domain Rules

- Paid prompt body exposure must always be decided on the server.
- The frontend must never receive full `PREMIUM` or `MASTER` prompt body before unlock permission is confirmed.
- `post_unlocks` is the source of truth for paid prompt access.
- Point deduction, unlock creation, and point history creation must happen in one transaction.
- Put point/unlock/reward flow in `commerce`.
- Put comments, likes, and bookmarks in `community`.
- Put prompt body, images, tags, and prompt statistics in `prompt`.
- Keep moderation independent because report/hide/review policy can grow quickly.

## Entity And Factory Rules

- Do not use setters on domain entities or JPA entities.
- Block direct construction with private/protected constructors.
- JPA entities should use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` and private builders when Lombok is available.
- Create new objects through static factory methods.
- Use explicit domain methods for state changes.

Factory method names:

- `create`: create a new object
- `of`: create from multiple values
- `from`: convert from one source object
- `reconstruct`: rebuild from persisted state

## Naming Conventions

- Domain entity: `Prompt`, `User`, `PointHistory`
- JPA entity: `{Domain}JpaEntity`
- UseCase: `{Action}{Domain}UseCase`
- Service: `{Domain}Service`, or `{Domain}CommandService` / `{Domain}QueryService`
- Outbound port: `{Domain}Repository`, `{Domain}Reader`, `{Domain}Writer`, or `{Action}{Domain}Port`
- Controller: `{Domain}Controller`
- Request/response DTO: suffix with `Request` or `Response`
- Application result: suffix with `Info`
- Spring Data repository: suffix with `JpaRepository`
- Persistence adapter: `{Domain}PersistenceAdapter`

## Read Method Semantics

- `get[By]`: data must exist; throw if not found.
- `find[By]`: data may not exist; return `Optional`.
- `list[By]`: return list, never null.
- `batchGet[By]`: all requested rows must exist; throw on missing rows.
- `search[By]`: complex filters or dynamic search.

Spring Data JPA repositories may use standard Spring Data method names.

## Testing

- Prefer focused domain and usecase tests.
- Domain tests should run without Spring where possible.
- Test paid prompt masking, point deduction, duplicate unlock prevention, and permission checks.
- Command services need `@Transactional`; query services need `@Transactional(readOnly = true)`.

## Git And PR

- Commit format: `<type>: <subject>`
- Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`
- Commit subjects may be written in Korean.
- Branch format: `type/#issue-number-english-summary`
- Branch examples: `feat/#2-entity-config-setting`, `docs/#8-update-pr-template`
- Quote branch names containing `#` in shell commands, for example `git push -u origin 'feat/#2-entity-config-setting'`.
- PR title format: `[Type] Korean summary`
- PR title examples: `[Feat] 프로젝트 초기 설정 및 엔티티 구성`, `[Docs] PR 템플릿 컨벤션 정리`
- Allowed PR tags: `[Feat]`, `[Fix]`, `[Hotfix]`, `[Refactor]`, `[Chore]`, `[Docs]`, `[Test]`
- Never add AI authorship or `Co-authored-by` trailers referencing AI agents.
