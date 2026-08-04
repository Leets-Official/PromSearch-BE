package com.promsearch.prompt.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.service.query.PromptTagQueryService;
import com.promsearch.prompt.application.usecase.dto.PromptTagInfo;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.Tag.TagId;
import com.promsearch.prompt.domain.enums.TagType;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptTagQueryServiceTest {

    private FakeLoadTagPort loadTagPort;
    private PromptTagQueryService promptTagQueryService;

    @BeforeEach
    void setUp() {
        loadTagPort = new FakeLoadTagPort();
        promptTagQueryService = new PromptTagQueryService(loadTagPort);
    }

    @Test
    void listByTypeReadsExistingTagsOnly() {
        List<PromptTagInfo> result = promptTagQueryService.listByType(TagType.JOB);

        assertThat(loadTagPort.lastTagType).isEqualTo(TagType.JOB);
        assertThat(result)
                .extracting(PromptTagInfo::name)
                .containsExactly("학생", "직장인");
    }

    private static class FakeLoadTagPort implements LoadTagPort {

        private TagType lastTagType;

        @Override
        public List<Tag> batchGetByIds(Collection<Long> tagIds) {
            return List.of();
        }

        @Override
        public List<Tag> listByType(TagType tagType) {
            this.lastTagType = tagType;
            return List.of(
                    Tag.reconstruct(new TagId(1L), tagType, "학생", "학생", false),
                    Tag.reconstruct(new TagId(2L), tagType, "직장인", "직장인", false)
            );
        }
    }
}
