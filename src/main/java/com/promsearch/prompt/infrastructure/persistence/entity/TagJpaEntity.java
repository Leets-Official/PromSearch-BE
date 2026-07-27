package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.Tag.TagId;
import com.promsearch.prompt.domain.enums.TagType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tags_type_name", columnNames = {"tag_type", "tag_name"}),
                @UniqueConstraint(
                        name = "uk_tags_type_normalized_name",
                        columnNames = {"tag_type", "normalized_name"}
                )
        }
)
public class TagJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false, length = 20)
    private TagType tagType;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "normalized_name", length = 100)
    private String normalizedName;

    @Column(name = "is_custom")
    private Boolean custom;

    @Builder(access = AccessLevel.PRIVATE)
    private TagJpaEntity(TagType tagType, String tagName, String normalizedName, Boolean custom) {
        this.tagType = tagType;
        this.tagName = tagName;
        this.normalizedName = normalizedName;
        this.custom = custom != null ? custom : false;
    }

    public static TagJpaEntity create(TagType tagType, String tagName, String normalizedName, Boolean custom) {
        return TagJpaEntity.builder()
                .tagType(tagType)
                .tagName(tagName)
                .normalizedName(normalizedName)
                .custom(custom)
                .build();
    }

    public static TagJpaEntity from(Tag tag) {
        return create(
                tag.getTagType(),
                tag.getTagName(),
                tag.getNormalizedName(),
                tag.isCustom()
        );
    }

    public Tag toDomain() {
        return Tag.reconstruct(
                new TagId(id),
                tagType,
                tagName,
                normalizedName,
                custom
        );
    }
}
