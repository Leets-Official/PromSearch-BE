alter table prompt_images add column etag varchar(255);
alter table prompt_images add column uploaded_at timestamp(6) with time zone;

alter table prompt_images drop constraint ck_prompt_images_status;
alter table prompt_images add constraint ck_prompt_images_status
    check (status in ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'FAILED'));

alter table prompt_images drop constraint ck_prompt_images_state_payload;
alter table prompt_images add constraint ck_prompt_images_state_payload check (
    (status = 'UPLOADING'
        and etag is null
        and uploaded_at is null
        and watermarked_object_key is null
        and failure_code is null
        and processing_version = 0)
    or (status = 'UPLOADED'
        and etag is not null
        and uploaded_at is not null
        and watermarked_object_key is null
        and failure_code is null
        and processing_version = 0)
    or (status = 'PROCESSING'
        and ((etag is null and uploaded_at is null) or (etag is not null and uploaded_at is not null))
        and watermarked_object_key is null
        and failure_code is null
        and processing_version = 0)
    or (status = 'READY'
        and ((etag is null and uploaded_at is null) or (etag is not null and uploaded_at is not null))
        and watermarked_object_key is not null
        and watermarked_object_key <> original_object_key
        and processing_version > 0
        and failure_code is null)
    or (status = 'FAILED'
        and ((etag is null and uploaded_at is null) or (etag is not null and uploaded_at is not null))
        and watermarked_object_key is null
        and failure_code is not null
        and processing_version = 0)
);

-- 기존 1차 브랜치에서 생성된 처리 데이터는 ETag/uploaded_at이 없을 수 있어 PROCESSING/READY/FAILED에서는
-- 두 컬럼이 모두 null인 레거시 상태를 허용한다. 새 업로드 흐름은 UPLOADED를 거치므로 항상 두 값을 저장한다.
