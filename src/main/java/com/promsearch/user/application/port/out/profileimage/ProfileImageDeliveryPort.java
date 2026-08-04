package com.promsearch.user.application.port.out.profileimage;

/**
 * 저장 형태에 따라 클라이언트에 전달할 프로필 이미지 URL을 결정하는 출력 포트.
 *
 * <p>직접 업로드한 이미지가 있으면 비공개 저장소의 서명 URL을 만들고, 아직 변경하지 않은
 * 소셜 로그인 사용자는 외부 제공자 URL을 그대로 사용할 수 있게 한다.</p>
 */
public interface ProfileImageDeliveryPort {

    /**
     * S3 객체 키를 우선하여 최종 프로필 이미지 URL을 해석한다.
     *
     * @param externalUrl 소셜 로그인 제공자가 전달한 외부 이미지 URL
     * @param objectKey 사용자가 직접 업로드한 이미지의 객체 키
     * @return 객체 키가 있으면 서명된 저장소 URL, 없으면 외부 URL
     */
    String resolve(String externalUrl, String objectKey);
}
