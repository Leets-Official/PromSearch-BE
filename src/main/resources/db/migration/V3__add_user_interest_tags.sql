ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN nickname TYPE VARCHAR(100);

CREATE TABLE user_interest_tags (
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT pk_user_interest_tags PRIMARY KEY (user_id, tag_id),
    CONSTRAINT fk_user_interest_tags_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_interest_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (tag_id)
);

CREATE INDEX idx_user_interest_tags_tag_id ON user_interest_tags (tag_id);

INSERT INTO tags (tag_type, tag_name, normalized_name, is_custom, created_at, updated_at)
VALUES
    ('JOB', '학생', '학생', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JOB', '직장인', '직장인', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JOB', '자영업자', '자영업자', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JOB', '기획자', '기획자', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JOB', '디자이너', '디자이너', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JOB', '개발자', '개발자', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', 'PPT', 'ppt', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', '레포트', '레포트', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', '이메일', '이메일', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', '보고서', '보고서', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', '회의록', '회의록', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TASK', '이미지 생성', '이미지 생성', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tag_type, tag_name) DO NOTHING;
