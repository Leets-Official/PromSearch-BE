package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterestTagSeedInitializer implements ApplicationRunner {

    private static final List<String> JOB_TAGS =
            List.of("학생", "직장인", "자영업자", "기획자", "디자이너", "개발자");
    private static final List<String> TASK_TAGS =
            List.of("PPT", "레포트", "이메일", "보고서", "회의록", "이미지 생성");

    private final InterestTagCatalogRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        synchronizeTagIdentity();
        seed(TagType.JOB, JOB_TAGS);
        seed(TagType.TASK, TASK_TAGS);
    }

    private void seed(TagType type, List<String> names) {
        names.stream()
                .filter(name -> !tagRepository.existsByTagTypeAndTagName(type, name))
                .map(name -> TagJpaEntity.create(type, name, name.toLowerCase(Locale.ROOT), false))
                .forEach(tagRepository::save);
    }

    private void synchronizeTagIdentity() {
        String databaseName = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        Long nextId = jdbcTemplate.queryForObject(
                "select coalesce(max(tag_id), 0) + 1 from tags",
                Long.class
        );

        if ("H2".equalsIgnoreCase(databaseName)) {
            jdbcTemplate.execute("alter table tags alter column tag_id restart with " + nextId);
            return;
        }
        if ("PostgreSQL".equalsIgnoreCase(databaseName)) {
            jdbcTemplate.execute("""
                    select setval(
                        pg_get_serial_sequence('tags', 'tag_id'),
                        (select coalesce(max(tag_id), 0) + 1 from tags),
                        false
                    )
                    """);
        }
    }
}
