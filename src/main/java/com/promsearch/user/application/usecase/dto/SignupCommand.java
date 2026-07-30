package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;
import com.promsearch.user.domain.NicknamePolicy;
import java.util.List;

public record SignupCommand(
        String name,
        String nickname,
        String email,
        String password,
        String profileImageUrl,
        List<String> jobTags,
        List<String> taskTags
) {

    public SignupCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(password);
        NicknamePolicy.validate(nickname);
        jobTags = normalizeTags(jobTags);
        taskTags = normalizeTags(taskTags);
    }

    public static SignupCommand of(String name, String nickname, String email, String password) {
        return new SignupCommand(name, nickname, email, password, null, List.of(), List.of());
    }

    public static SignupCommand of(
            String name,
            String nickname,
            String email,
            String password,
            String profileImageUrl,
            List<String> jobTags,
            List<String> taskTags
    ) {
        return new SignupCommand(name, nickname, email, password, profileImageUrl, jobTags, taskTags);
    }

    private static List<String> normalizeTags(List<String> tags) {
        return tags == null
                ? List.of()
                : tags.stream().map(tag -> tag == null ? null : tag.trim()).toList();
    }

    @Override
    public String toString() {
        return "SignupCommand[name=" + name + ", nickname=" + nickname + ", email=" + email
                + ", password=***, profileImageUrl=" + profileImageUrl
                + ", jobTags=" + jobTags + ", taskTags=" + taskTags + "]";
    }
}
