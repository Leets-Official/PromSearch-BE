-- 기본 AI 모델 태그는 프론트가 안정적인 tagId로 사용할 수 있도록
-- 기존 V1 시드(1~12) 다음의 명시적인 ID를 사용한다.
-- '기타' 모델은 공용 태그로 저장하지 않고, customAiModel 입력으로 별도 생성한다.
insert into tags (
    tag_id,
    tag_type,
    tag_name,
    normalized_name,
    is_custom,
    created_at,
    updated_at
)
values
    (13, 'AI_MODEL', 'ChatGPT', 'chatgpt', false, current_timestamp, current_timestamp),
    (14, 'AI_MODEL', 'Gemini', 'gemini', false, current_timestamp, current_timestamp),
    (15, 'AI_MODEL', 'Claude', 'claude', false, current_timestamp, current_timestamp);

-- 명시적 ID 삽입 후 다음 자동 생성 ID가 충돌하지 않도록 시퀀스를 맞춘다.
select setval(
    pg_get_serial_sequence('tags', 'tag_id'),
    (select max(tag_id) from tags),
    true
);
