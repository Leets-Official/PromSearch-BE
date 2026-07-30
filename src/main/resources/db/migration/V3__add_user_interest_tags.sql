ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN nickname TYPE VARCHAR(10);

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
