package com.promsearch.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InitialSchemaMigrationTest {

    private static final String MIGRATION_PATH = "db/migration/V1__create_initial_schema.sql";

    @DisplayName("초기 마이그레이션은 빈 DB에 전체 테이블과 FK를 생성한다")
    @Test
    void migrationCreatesCurrentSchemaAndForeignKeys() throws Exception {
        String databaseUrl = "jdbc:h2:mem:initial_schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            executeMigration(connection);

            assertThat(tableNames(connection)).contains(
                    "users",
                    "refresh_token_sessions",
                    "social_accounts",
                    "posts",
                    "tags",
                    "user_interest_tags",
                    "post_tags",
                    "prompt_images",
                    "prompt_image_watermark_outbox",
                    "post_statistics",
                    "comments",
                    "post_interactions",
                    "point_histories",
                    "post_unlocks",
                    "post_copies",
                    "post_reports",
                    "comment_reports",
                    "event_logs"
            );

            assertThat(foreignKeys(connection)).contains(
                    "refresh_token_sessions.user_id->users.user_id",
                    "social_accounts.user_id->users.user_id",
                    "posts.user_id->users.user_id",
                    "user_interest_tags.user_id->users.user_id",
                    "user_interest_tags.tag_id->tags.tag_id",
                    "post_tags.post_id->posts.post_id",
                    "post_tags.tag_id->tags.tag_id",
                    "prompt_images.uploader_id->users.user_id",
                    "prompt_images.prompt_id->posts.post_id",
                    "prompt_image_watermark_outbox.image_id->prompt_images.prompt_image_id",
                    "post_statistics.post_id->posts.post_id",
                    "comments.post_id->posts.post_id",
                    "comments.user_id->users.user_id",
                    "comments.parent_comment_id->comments.comment_id",
                    "post_interactions.user_id->users.user_id",
                    "post_interactions.post_id->posts.post_id",
                    "point_histories.user_id->users.user_id",
                    "post_unlocks.post_id->posts.post_id",
                    "post_unlocks.user_id->users.user_id",
                    "post_unlocks.creator_user_id->users.user_id",
                    "post_copies.user_id->users.user_id",
                    "post_copies.post_id->posts.post_id",
                    "post_reports.reporter_id->users.user_id",
                    "post_reports.post_id->posts.post_id",
                    "comment_reports.reporter_id->users.user_id",
                    "comment_reports.comment_id->comments.comment_id"
            );
            assertThat(foreignKeys(connection))
                    .noneMatch(foreignKey -> foreignKey.startsWith("event_logs."));
        }
    }

    @DisplayName("초기 마이그레이션은 FK와 주요 도메인 제약조건을 강제한다")
    @Test
    void migrationEnforcesForeignKeysAndDomainConstraints() throws Exception {
        String databaseUrl = "jdbc:h2:mem:initial_schema_constraints;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            executeMigration(connection);
            insertUser(connection, 1L, "creator@example.com", "creator");
            insertUser(connection, 2L, "buyer@example.com", "buyer");
            insertPost(connection, 1L, 1L, "ACTIVE", "PUBLIC", null);

            assertThatThrownBy(() -> execute(connection, """
                    insert into point_histories (
                        user_id, point_transaction_type, amount, balance_after, created_at, updated_at
                    ) values (999, 'SIGNUP_REWARD', 100, 100, current_timestamp, current_timestamp)
                    """))
                    .isInstanceOf(SQLException.class);

            insertPost(connection, 2L, 1L, "DRAFT", "PRIVATE", null);
            assertThatThrownBy(() -> insertPost(connection, 3L, 1L, "DRAFT", "PRIVATE", null))
                    .isInstanceOf(SQLException.class);
            insertPost(connection, 4L, 1L, "DRAFT", "PRIVATE", "current_timestamp");

            assertThatThrownBy(() -> insertPost(connection, 5L, 1L, "ACTIVE", "FRIENDS", null))
                    .isInstanceOf(SQLException.class);

            String imageId = UUID.randomUUID().toString();
            execute(connection, """
                    insert into prompt_images (
                        prompt_image_id, uploader_id, original_object_key, original_file_name,
                        content_type, file_size, width, height, etag, uploaded_at, status,
                        processing_version, is_thumbnail, lock_version, created_at, updated_at
                    ) values (
                        '%s', 1, 'prompt-images/original/1/valid.jpg', 'valid.jpg',
                        'JPEG', 1024, 1920, 1080, '"etag"', current_timestamp, 'UPLOADED',
                        0, false, 0, current_timestamp, current_timestamp
                    )
                    """.formatted(imageId));

            assertThatThrownBy(() -> execute(connection, """
                    insert into prompt_images (
                        prompt_image_id, uploader_id, original_object_key, original_file_name,
                        content_type, file_size, width, height, status,
                        processing_version, is_thumbnail, lock_version, created_at, updated_at
                    ) values (
                        random_uuid(), 1, 'prompt-images/original/1/invalid.webp', 'invalid.webp',
                        'WEBP', 1024, 1920, 1080, 'UPLOADING',
                        0, false, 0, current_timestamp, current_timestamp
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void executeMigration(Connection connection) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MIGRATION_PATH)) {
            assertThat(inputStream).as("migration resource %s", MIGRATION_PATH).isNotNull();
            String migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            for (String rawStatement : migration.split(";")) {
                String statement = rawStatement.trim();
                if (statement.isBlank()) {
                    continue;
                }
                if (statement.contains("idx_users_profile_image_object_key")) {
                    execute(connection, "create index idx_users_profile_image_object_key "
                            + "on users (profile_image_object_key)");
                    continue;
                }
                if (statement.contains("uk_posts_user_live_draft")) {
                    execute(connection, """
                            alter table posts add column live_draft_user_id bigint
                                generated always as (
                                    case when status = 'DRAFT' and deleted_at is null then user_id else null end
                                )
                            """);
                    execute(connection, "create unique index uk_posts_user_live_draft "
                            + "on posts (live_draft_user_id)");
                    continue;
                }
                execute(connection, statement);
            }
        }
    }

    private Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> tableNames = new HashSet<>();
        try (ResultSet tables = connection.getMetaData().getTables(null, "public", null, new String[]{"TABLE"})) {
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return tableNames;
    }

    private Set<String> foreignKeys(Connection connection) throws SQLException {
        Set<String> foreignKeys = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        for (String table : tableNames(connection)) {
            try (ResultSet importedKeys = metadata.getImportedKeys(null, "public", table)) {
                while (importedKeys.next()) {
                    foreignKeys.add((importedKeys.getString("FKTABLE_NAME") + "."
                            + importedKeys.getString("FKCOLUMN_NAME") + "->"
                            + importedKeys.getString("PKTABLE_NAME") + "."
                            + importedKeys.getString("PKCOLUMN_NAME")).toLowerCase(Locale.ROOT));
                }
            }
        }
        return foreignKeys;
    }

    private void insertUser(Connection connection, long userId, String email, String nickname) throws SQLException {
        execute(connection, """
                insert into users (
                    user_id, email, password, nickname, name, points, role, grade, status,
                    created_at, updated_at
                ) values (
                    %d, '%s', 'password', '%s', '%s', 0, 'USER', 'NODE', 'ACTIVE',
                    current_timestamp, current_timestamp
                )
                """.formatted(userId, email, nickname, nickname));
    }

    private void insertPost(Connection connection, long postId, long userId, String status,
                            String visibility, String deletedAt) throws SQLException {
        String deletedAtValue = deletedAt == null ? "null" : deletedAt;
        execute(connection, """
                insert into posts (
                    post_id, user_id, title, status, visibility, created_at, updated_at, deleted_at
                ) values (
                    %d, %d, 'title', '%s', '%s', current_timestamp, current_timestamp, %s
                )
                """.formatted(postId, userId, status, visibility, deletedAtValue));
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
