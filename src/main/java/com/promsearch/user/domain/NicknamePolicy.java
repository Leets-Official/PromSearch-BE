package com.promsearch.user.domain;

import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.regex.Pattern;

public final class NicknamePolicy {

    public static final int MAX_LENGTH = 10;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]+$");

    private NicknamePolicy() {
    }

    public static void validate(String nickname) {
        if (nickname == null
                || nickname.isBlank()
                || nickname.length() > MAX_LENGTH
                || !ALLOWED_PATTERN.matcher(nickname).matches()) {
            throw new UserDomainException(UserErrorCode.INVALID_NICKNAME);
        }
    }
}
