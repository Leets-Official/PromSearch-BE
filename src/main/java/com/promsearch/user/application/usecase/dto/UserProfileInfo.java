package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.application.port.out.tag.InterestTagRow;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.InterestTagType;
import java.util.List;

/**
 * 내 프로필 조회 UseCase가 API 계층에 전달하는 읽기 모델.
 *
 * <p>{@code profileImageUrl}은 외부 제공자 URL 또는 S3 Presigned GET URL로 해석된 값이다.</p>
 */
public record UserProfileInfo(
        String username,
        String profileImageUrl,
        String email,
        Long point,
        String gradeName,
        List<InterestTagInfo> jobTags,
        List<InterestTagInfo> taskTags
) {

    /**
     * 사용자 도메인 정보와 저장소 전달 정책으로 해석된 이미지 URL, 현재 관심 태그를 결합한다.
     */
    public static UserProfileInfo from(User user, String profileImageUrl, List<InterestTagRow> interestTags) {
        return new UserProfileInfo(
                user.getNickname(),
                profileImageUrl,
                user.getEmail(),
                user.getPoint(),
                user.getGrade().name(),
                interestTags.stream()
                        .filter(row -> row.type() == InterestTagType.JOB)
                        .map(InterestTagInfo::from)
                        .toList(),
                interestTags.stream()
                        .filter(row -> row.type() == InterestTagType.TASK)
                        .map(InterestTagInfo::from)
                        .toList()
        );
    }
}
