package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.domain.Prompt;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptCreationMigrationTest {

    private static final String CREATION_MIGRATION_PATH = "db/migration/V6__add_prompt_creation_fields.sql";
    private static final String DRAFT_MIGRATION_PATH = "db/migration/V7__add_single_live_prompt_draft_constraint.sql";

    @DisplayName("생성 필드 마이그레이션은 공개 범위와 custom 태그 정규화 중복을 제한한다")
    @Test
    void migrationAddsPromptCreationConstraints() throws Exception {
        String databaseUrl = "jdbc:h2:mem:prompt_creation_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            execute(connection, """
                    create table posts (
                        post_id bigint primary key,
                        user_id bigint,
                        title varchar(20),
                        status varchar(20),
                        deleted_at timestamp
                    )
                    """);
            execute(connection, """
                    create table tags (
                        tag_id bigint primary key,
                        tag_type varchar(20) not null,
                        tag_name varchar(100) not null,
                        normalized_name varchar(100)
                    )
                    """);
            execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (1, 'AI_MODEL', 'GPT 4.1 Mini', null)
                    """);
            executeMigration(connection);

            execute(connection, "insert into posts (post_id, title) values (1, '"
                    + "가".repeat(Prompt.MAX_TITLE_LENGTH) + "')");
            try (ResultSet resultSet = query(
                    connection,
                    "select visibility from posts where post_id = 1"
            )) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("visibility")).isEqualTo("PUBLIC");
            }

            assertThatThrownBy(() -> execute(
                    connection,
                    "insert into posts (post_id, visibility) values (2, 'FRIENDS')"
            )).isInstanceOf(SQLException.class);

            try (ResultSet resultSet = query(
                    connection,
                    "select normalized_name from tags where tag_id = 1"
            )) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("normalized_name")).isEqualTo("gpt4.1mini");
            }

            execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (2, 'AI_MODEL', 'Claude', 'claude')
                    """);
            assertThatThrownBy(() -> execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (3, 'AI_MODEL', 'gpt4.1mini', 'gpt4.1mini')
                    """)).isInstanceOf(SQLException.class);

            execute(connection, "insert into posts (post_id, user_id, title, status) values (10, 1, 'draft1', 'DRAFT')");
            assertThatThrownBy(() -> execute(
                    connection,
                    "insert into posts (post_id, user_id, title, status) values (11, 1, 'draft2', 'DRAFT')"
            )).isInstanceOf(SQLException.class);
            execute(connection, """
                    insert into posts (post_id, user_id, title, status, deleted_at)
                    values (12, 1, 'deleted draft', 'DRAFT', timestamp '2026-07-28 00:00:00')
                    """);
            execute(connection, "insert into posts (post_id, user_id, title, status) values (13, 1, 'active', 'ACTIVE')");
        }
    }

    private void executeMigration(Connection connection) throws Exception {
        executeMigration(connection, CREATION_MIGRATION_PATH);
        executeMigration(connection, DRAFT_MIGRATION_PATH);
    }

    private void executeMigration(Connection connection, String migrationPath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(migrationPath)) {
            assertThat(inputStream).as("migration resource %s", migrationPath).isNotNull();
            String migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (DRAFT_MIGRATION_PATH.equals(migrationPath)) {
                assertThat(migration)
                        .contains("where status = 'DRAFT' and deleted_at is null");
                execute(connection, """
                        alter table posts add column live_draft_user_id bigint
                            generated always as (
                                case when status = 'DRAFT' and deleted_at is null then user_id else null end
                            )
                        """);
                execute(connection, "create unique index uk_posts_user_live_draft on posts (live_draft_user_id)");
                return;
            }
            for (String statement : migration.split(";")) {
                if (!statement.isBlank()) {
                    execute(connection, statement);
                }
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private ResultSet query(Connection connection, String sql) throws SQLException {
        return connection.createStatement().executeQuery(sql);
    }
}
