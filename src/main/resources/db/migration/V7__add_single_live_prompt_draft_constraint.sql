create unique index uk_posts_user_live_draft
    on posts (user_id)
    where status = 'DRAFT' and deleted_at is null;
