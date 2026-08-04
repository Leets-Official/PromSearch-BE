package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;
import com.promsearch.user.domain.NicknamePolicy;
import java.util.List;

public record SignupCommand(
        String nickname,
        String email,
        String password,
        String profileImageUrl,
        List<Long> interestJobTagIds,
        List<Long> interestTaskTagIds
) {

    public SignupCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(password);
        NicknamePolicy.validate(nickname);
        interestJobTagIds = normalizeTagIds(interestJobTagIds);
        interestTaskTagIds = normalizeTagIds(interestTaskTagIds);
    }

    public static SignupCommand of(String nickname, String email, String password) {
        return new SignupCommand(nickname, email, password, null, List.of(), List.of());
    }

    public static SignupCommand of(
            String nickname,
            String email,
            String password,
            String profileImageUrl,
            List<Long> interestJobTagIds,
            List<Long> interestTaskTagIds
    ) {
        return new SignupCommand(nickname, email, password, profileImageUrl, interestJobTagIds, interestTaskTagIds);
    }

    private static List<Long> normalizeTagIds(List<Long> tagIds) {
        return tagIds == null
                ? List.of()
                : List.copyOf(tagIds);
    }

    @Override
    public String toString() {
        return "SignupCommand[nickname=" + nickname + ", email=" + email
                + ", password=***, profileImageUrl=" + profileImageUrl + ", interestJobTagIds=" + interestJobTagIds
                + ", interestTaskTagIds=" + interestTaskTagIds + "]";
    }
}
