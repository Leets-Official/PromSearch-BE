package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;
import com.promsearch.user.domain.NicknamePolicy;
import java.util.List;

public record SignupCommand(
        String nickname,
        String email,
        String password,
        List<Long> jobTagIds,
        List<Long> taskTagIds
) {

    public SignupCommand {
        CredentialPolicy.validateEmail(email);
        CredentialPolicy.validatePassword(password);
        NicknamePolicy.validate(nickname);
        jobTagIds = normalizeTagIds(jobTagIds);
        taskTagIds = normalizeTagIds(taskTagIds);
    }

    public static SignupCommand of(String nickname, String email, String password) {
        return new SignupCommand(nickname, email, password, List.of(), List.of());
    }

    public static SignupCommand of(
            String nickname,
            String email,
            String password,
            List<Long> jobTagIds,
            List<Long> taskTagIds
    ) {
        return new SignupCommand(nickname, email, password, jobTagIds, taskTagIds);
    }

    private static List<Long> normalizeTagIds(List<Long> tagIds) {
        return tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    @Override
    public String toString() {
        return "SignupCommand[nickname=" + nickname + ", email=" + email
                + ", password=***, jobTagIds=" + jobTagIds + ", taskTagIds=" + taskTagIds + "]";
    }
}
