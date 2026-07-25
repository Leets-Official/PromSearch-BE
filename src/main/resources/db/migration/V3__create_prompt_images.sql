create table prompt_images (
    prompt_image_id uuid primary key,
    uploader_id bigint not null,
    prompt_id bigint,
    original_object_key varchar(1024) not null,
    watermarked_object_key varchar(1024),
    original_file_name varchar(255) not null,
    content_type varchar(10) not null,
    file_size bigint not null,
    width integer not null,
    height integer not null,
    status varchar(20) not null,
    processing_version integer not null default 0,
    failure_code varchar(100),
    sort_order integer,
    is_thumbnail boolean not null default false,
    lock_version bigint not null default 0,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    deleted_at timestamp(6) with time zone,
    constraint uk_prompt_images_original_object_key unique (original_object_key),
    constraint uk_prompt_images_watermarked_object_key unique (watermarked_object_key),
    constraint uk_prompt_images_prompt_sort_order unique (prompt_id, sort_order),
    constraint ck_prompt_images_content_type check (content_type in ('JPEG', 'PNG')),
    constraint ck_prompt_images_file_size check (file_size > 0 and file_size <= 10485760),
    constraint ck_prompt_images_dimensions check (
        width > 0
        and height > 0
        and width <= 8192
        and height <= 8192
        and cast(width as bigint) * cast(height as bigint) <= 40000000
    ),
    constraint ck_prompt_images_status check (status in ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),
    constraint ck_prompt_images_processing_version check (processing_version >= 0),
    constraint ck_prompt_images_state_payload check (
        (status in ('UPLOADING', 'PROCESSING')
            and watermarked_object_key is null
            and failure_code is null
            and processing_version = 0)
        or (status = 'READY'
            and watermarked_object_key is not null
            and watermarked_object_key <> original_object_key
            and processing_version > 0
            and failure_code is null)
        or (status = 'FAILED'
            and watermarked_object_key is null
            and failure_code is not null
            and processing_version = 0)
    ),
    constraint ck_prompt_images_attachment check (
        (prompt_id is null and sort_order is null and is_thumbnail = false)
        or (prompt_id is not null and sort_order is not null and sort_order >= 0 and status = 'READY')
    )
);

-- uploader_id와 prompt_id의 FK는 users/posts를 포함한 전체 Flyway baseline이 정리되는 #21 이후 추가한다.
-- 현재는 업로드 전 자산을 독립적으로 저장하고, application 계층에서 소유권과 연결 가능 상태를 검증한다.
create index idx_prompt_images_uploader_status on prompt_images (uploader_id, status);
create index idx_prompt_images_status_created_at on prompt_images (status, created_at);
