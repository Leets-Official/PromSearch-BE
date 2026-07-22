package com.promsearch.auth.domain;

import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.net.IDN;

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

    private static final int EMAIL_LOCAL_PART_MAX_LENGTH = 64;
    private static final int EMAIL_DOMAIN_MAX_LENGTH = 253;
    private static final int EMAIL_DOMAIN_LABEL_MAX_LENGTH = 63;
    private static final String LOCAL_PART_SPECIAL_CHARACTERS = "!#$%&'*+-/=?^_`{|}~";

    private CredentialPolicy() {
    }

    /**
     * 신규 가입이나 이메일 변경 시 저장 가능한 이메일인지 검증한다.
     */
    public static void validateEmail(String email) {
        if (email == null || email.length() > EMAIL_MAX_LENGTH) {
            throwInvalidEmailFormat();
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@') || atIndex == email.length() - 1) {
            throwInvalidEmailFormat();
        }

        validateLocalPart(email.substring(0, atIndex));
        validateDomain(email.substring(atIndex + 1));
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

    /**
     * RFC 5322의 일반적인 dot-atom 형식을 따른다.
     * <p>
     * 연속된 점과 앞뒤 점은 차단하되, apostrophe와 {@code +}, {@code /}, {@code =} 등
     * 실제 메일 주소에서 쓰이는 특수문자 및 국제화 로컬 파트는 허용한다.
     */
    private static void validateLocalPart(String localPart) {
        int codePointLength = localPart.codePointCount(0, localPart.length());
        if (codePointLength == 0
                || codePointLength > EMAIL_LOCAL_PART_MAX_LENGTH
                || localPart.startsWith(".")
                || localPart.endsWith(".")
                || localPart.contains("..")) {
            throwInvalidEmailFormat();
        }

        boolean valid = localPart.codePoints().allMatch(CredentialPolicy::isLocalPartCharacter);
        if (!valid) {
            throwInvalidEmailFormat();
        }
    }

    /**
     * 국제화 도메인은 IDN 규칙으로 punycode 변환한 뒤 DNS label 제약을 검증한다.
     * 정규식 하나로 모든 문법을 흉내 내는 대신 JDK 표준 변환기를 사용해 허용 범위를 넓히면서
     * 선행/후행 하이픈, 빈 label 같은 잘못된 도메인은 차단한다.
     */
    private static void validateDomain(String domain) {
        final String asciiDomain;
        try {
            asciiDomain = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException exception) {
            throwInvalidEmailFormat();
            return;
        }

        if (asciiDomain.length() > EMAIL_DOMAIN_MAX_LENGTH
                || asciiDomain.startsWith(".")
                || asciiDomain.endsWith(".")
                || !asciiDomain.contains(".")) {
            throwInvalidEmailFormat();
        }

        String[] labels = asciiDomain.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty()
                    || label.length() > EMAIL_DOMAIN_LABEL_MAX_LENGTH
                    || label.startsWith("-")
                    || label.endsWith("-")
                    || !label.chars().allMatch(CredentialPolicy::isDomainLabelCharacter)) {
                throwInvalidEmailFormat();
            }
        }
    }

    private static boolean isLocalPartCharacter(int codePoint) {
        int characterType = Character.getType(codePoint);
        return codePoint == '.'
                || Character.isLetterOrDigit(codePoint)
                || characterType == Character.NON_SPACING_MARK
                || characterType == Character.COMBINING_SPACING_MARK
                || (codePoint < 128 && LOCAL_PART_SPECIAL_CHARACTERS.indexOf(codePoint) >= 0);
    }

    private static boolean isDomainLabelCharacter(int character) {
        return (character >= 'A' && character <= 'Z')
                || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9')
                || character == '-';
    }

    private static void throwInvalidEmailFormat() {
        throw new AuthDomainException(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }
}
