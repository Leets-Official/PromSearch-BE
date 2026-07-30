create table prompt_image_watermark_outbox (
    event_id uuid primary key,
    image_id uuid not null,
    event_type varchar(100) not null,
    event_version integer not null,
    processing_version integer not null,
    payload text not null,
    status varchar(20) not null,
    attempt_count integer not null default 0,
    available_at timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,
    last_error varchar(1000),
    lock_version bigint not null default 0,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    deleted_at timestamp(6) with time zone,
    constraint fk_prompt_image_watermark_outbox_image
        foreign key (image_id) references prompt_images (prompt_image_id) on delete cascade,
    constraint uk_prompt_image_watermark_outbox_image_version
        unique (image_id, processing_version),
    constraint ck_prompt_image_watermark_outbox_event_version
        check (event_version > 0),
    constraint ck_prompt_image_watermark_outbox_processing_version
        check (processing_version > 0),
    constraint ck_prompt_image_watermark_outbox_status
        check (status in ('PENDING', 'PUBLISHED')),
    constraint ck_prompt_image_watermark_outbox_attempt_count
        check (attempt_count >= 0),
    constraint ck_prompt_image_watermark_outbox_publish_state check (
        (status = 'PENDING' and published_at is null)
        or (status = 'PUBLISHED' and published_at is not null)
    )
);

create index idx_prompt_image_watermark_outbox_publish
    on prompt_image_watermark_outbox (status, available_at, created_at);
