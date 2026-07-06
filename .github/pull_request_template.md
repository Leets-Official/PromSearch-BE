<!--
## PR 제목 / 브랜치 컨벤션

> PR 제목은 `[Type] 한국어 요약` 형식으로 작성해주세요.
> 브랜치는 `type/#이슈번호-영문-요약` 형식으로 생성해주세요.

PR 제목 예시:

- `[Feat] 프로젝트 초기 설정 및 엔티티 구성`
- `[Docs] PR 템플릿 컨벤션 정리`
- `[Fix] 프롬프트 상세 조회 권한 검증 수정`
- `[Chore] GitHub Actions 설정 추가`

브랜치명 예시:

- `feat/#2-entity-config-setting`
- `docs/#8-update-pr-template`
- `fix/#15-prompt-access-check`
- `chore/#21-github-actions-setting`
-->

## 체크리스트

- [ ] 병합 대상 브랜치가 올바른지 확인했습니다. (`develop` 또는 팀에서 합의한 개발 브랜치)
- [ ] PR 제목을 컨벤션에 맞게 작성했습니다. 예: `[Feat] 프롬프트 언락 기능 추가`
- [ ] 브랜치명을 컨벤션에 맞게 작성했습니다. 예: `feat/#2-entity-config-setting`
- [ ] 관련 이슈를 연결했습니다.
- [ ] 로컬에서 `./gradlew test` 또는 `./gradlew build`를 실행했습니다.
- [ ] API 응답에 민감 정보, 토큰, 유료 프롬프트 원문이 노출되지 않는지 확인했습니다.

## 연관 이슈

> `closes`, `resolves`, `fixes`, `related to`를 사용해 Issue와 PR을 연결해주세요.

- resolves #

## 작업 내용

> 무엇을 왜 변경했는지 설명해주세요. 정책 결정, API 명세, ERD, 회의록 등 근거 문서가 있다면 링크를 함께 첨부해주세요.

1.

## 변경 범위

- [ ] `auth`
- [ ] `user`
- [ ] `prompt`
- [ ] `commerce`
- [ ] `community`
- [ ] `moderation`
- [ ] `tracking`
- [ ] `admin`
- [ ] `common`
- [ ] `global`
- [ ] 문서 / 설정 / 배포

## 리뷰 중점사항

> 리뷰어가 특히 확인해야 할 부분을 적어주세요.

-

## 테스트

> 실행한 테스트와 확인 결과를 적어주세요.

- [ ] `./gradlew test`
- [ ] `./gradlew build`
- [ ] 별도 확인:

## 스크린샷 / 응답 예시

> API 응답, Swagger, 로그, 화면 캡처 등 확인에 필요한 자료가 있다면 첨부해주세요.
