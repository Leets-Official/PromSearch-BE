package com.promsearch.common.infrastructure.storage;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 여러 이미지 도메인이 동일한 객체 키 규칙을 사용하도록 만드는 공통 팩토리.
 *
 * <p>객체 키는 {@code {prefix}/{ownerId}/{objectId}.{extension}} 형식으로 생성한다.</p>
 */
@Component
public class StorageObjectKeyFactory {

    /**
     * 소유자 단위로 분리된 객체 키를 생성한다.
     *
     * @param prefix 이미지 용도를 구분하는 경로 접두사
     * @param ownerId 객체를 소유한 사용자 식별자
     * @param objectId 객체마다 고유한 식별자
     * @param extension 점을 제외한 파일 확장자
     * @return 정규화된 객체 키
     */
    public String generate(String prefix, Long ownerId, UUID objectId, String extension) {
        return "%s/%d/%s.%s".formatted(normalize(prefix), ownerId, objectId, extension);
    }

    /**
     * 객체 키가 지정된 접두사와 소유자 경로 아래에 있는지 확인한다.
     *
     * <p>다른 사용자가 발급받은 객체 키를 업로드 완료 요청에 사용하는 것을 차단하는
     * 1차 소유권 검사로 사용한다.</p>
     *
     * @param objectKey 검사할 객체 키
     * @param prefix 이미지 용도를 구분하는 경로 접두사
     * @param ownerId 요청 사용자 식별자
     * @return 해당 사용자의 경로에 속하면 {@code true}
     */
    public boolean isOwnedBy(String objectKey, String prefix, Long ownerId) {
        if (objectKey == null || prefix == null || ownerId == null) {
            return false;
        }
        return objectKey.startsWith(normalize(prefix) + "/" + ownerId + "/");
    }

    private String normalize(String prefix) {
        return prefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
