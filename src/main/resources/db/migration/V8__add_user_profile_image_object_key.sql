alter table users
    add column if not exists profile_image_object_key varchar(1024);

create index if not exists idx_users_profile_image_object_key
    on users (profile_image_object_key)
    where profile_image_object_key is not null;
