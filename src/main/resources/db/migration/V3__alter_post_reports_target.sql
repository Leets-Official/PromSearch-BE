alter table post_reports add column target_type varchar(20);
alter table post_reports add column target_id bigint;

update post_reports set target_type = 'POST', target_id = post_id where target_type is null;

alter table post_reports alter column target_type set not null;
alter table post_reports alter column target_id set not null;

alter table post_reports drop constraint uk_post_reports_user_post;
alter table post_reports add constraint uk_post_reports_reporter_target unique (reporter_id, target_type, target_id);

alter table post_reports drop column post_id;
