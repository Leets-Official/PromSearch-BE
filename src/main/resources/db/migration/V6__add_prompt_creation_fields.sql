alter table posts alter column title type varchar(500);

alter table posts add column visibility varchar(20) not null default 'PUBLIC';

alter table posts add constraint ck_posts_visibility
    check (visibility in ('PUBLIC', 'PRIVATE'));

-- 애플리케이션의 AI 모델 태그 정규화 규칙(공백 제거 + 소문자)에 맞춰 기존 행을 보정한다.
-- 보정 결과가 겹치면 아래 unique 제약 추가가 실패하도록 하여 중복 태그를 묵시적으로 병합하지 않는다.
update tags
set normalized_name = lower(regexp_replace(tag_name, '\s+', '', 'g'))
where tag_type = 'AI_MODEL';

alter table tags add constraint uk_tags_type_normalized_name
    unique (tag_type, normalized_name);
