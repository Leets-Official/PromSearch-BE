package com.promsearch.auth.domain;

import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.util.regex.Pattern;

/**
 * 이메일과 비밀번호 형식에 대한 인증 도메인의 단일 진실 원천(SSOT).
 * <p>
 * HTTP 요청 검증에 정책을 가두지 않고 Command 생성 경계에서 적용해,
 * 향후 배치나 내부 API가 추가되더라도 동일한 자격증명 규칙을 보장한다.
 */
public final class CredentialPolicy {

    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;

    /**
     * 서비스에서 허용하는 이메일 형식의 실용적인 부분 집합.
     * 국제화 이메일 주소보다 현재 사용자/DB 계약에 맞는 ASCII 주소를 명시적으로 허용한다.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private CredentialPolicy() {
    }

    /**
     * 신규 가입이나 이메일 변경 시 저장 가능한 이메일인지 검증한다.
     */
    public static void validateEmail(String email) {
        if (email == null
                || email.length() > EMAIL_MAX_LENGTH
                || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new AuthDomainException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    /**
     * 새로 저장할 평문 비밀번호가 길이와 복잡도 정책을 만족하는지 검증한다.
     * <p>
     * 영문, 숫자, ASCII 특수문자 중 두 종류 이상을 요구하며 공백, 제어문자,
     * 한글 등 정의된 범주 밖의 문자는 허용하지 않는다.
     */
    public static void validatePassword(String rawPassword) {
        if (rawPassword == null
                || rawPassword.length() < PASSWORD_MIN_LENGTH
                || rawPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new AuthDomainException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;

        for (int i = 0; i < rawPassword.length(); i++) {
            char character = rawPassword.charAt(i);
            if (isEnglishLetter(character)) {
                hasLetter = true;
            } else if (isDigit(character)) {
                hasDigit = true;
            } else if (isAsciiSpecialCharacter(character)) {
                hasSpecialCharacter = true;
            } else {
                throw new AuthDomainException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
            }
        }

        int categoryCount = (hasLetter ? 1 : 0)
                + (hasDigit ? 1 : 0)
                + (hasSpecialCharacter ? 1 : 0);
        if (categoryCount < 2) {
            throw new AuthDomainException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
        }
    }

    private static boolean isEnglishLetter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isAsciiSpecialCharacter(char character) {
        return (character >= '!' && character <= '/')
                || (character >= ':' && character <= '@')
                || (character >= '[' && character <= '`')
                || (character >= '{' && character <= '~');
    }
}
