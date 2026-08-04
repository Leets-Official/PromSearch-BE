package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;
import com.promsearch.user.domain.NicknamePolicy;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.List;

public record SignupCommand(
        String nickname,
        String email,
        String password,
        String profileImageUrl,
        List<String> jobTags,
        List<String> taskTags,
        SignupAgreements agreements
) {

    public SignupCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(password);
        NicknamePolicy.validate(nickname);
        jobTags = normalizeTags(jobTags);
        taskTags = normalizeTags(taskTags);
        if (agreements == null) {
            throw new UserDomainException(UserErrorCode.INVALID_AGREEMENT);
        }
    }

    public static SignupCommand of(String nickname, String email, String password) {
        return new SignupCommand(nickname, email, password, null, List.of(), List.of(), SignupAgreements.requiredAndNoMarketing());
    }

    public static SignupCommand of(
            String nickname,
            String email,
            String password,
            String profileImageUrl,
            List<String> jobTags,
            List<String> taskTags
    ) {
        return new SignupCommand(nickname, email, password, profileImageUrl, jobTags, taskTags,
                SignupAgreements.requiredAndNoMarketing());
    }

    public static SignupCommand of(
            String nickname,
            String email,
            String password,
            String profileImageUrl,
            List<String> jobTags,
            List<String> taskTags,
            SignupAgreements agreements
    ) {
        return new SignupCommand(nickname, email, password, profileImageUrl, jobTags, taskTags, agreements);
    }

    private static List<String> normalizeTags(List<String> tags) {
        return tags == null
                ? List.of()
                : tags.stream().map(tag -> tag == null ? null : tag.trim()).toList();
    }

    @Override
    public String toString() {
        return "SignupCommand[nickname=" + nickname + ", email=" + email
                + ", password=***, profileImageUrl=" + profileImageUrl
                + ", jobTags=" + jobTags + ", taskTags=" + taskTags + ", agreements=" + agreements + "]";
    }
}
