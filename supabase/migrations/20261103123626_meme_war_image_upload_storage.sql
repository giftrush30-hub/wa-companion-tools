alter table public.arena_meme_submissions
  add column if not exists image_url text;
alter table public.arena_meme_submissions
  drop constraint if exists arena_meme_submissions_image_url_check;
alter table public.arena_meme_submissions
  add constraint arena_meme_submissions_image_url_check
  check (image_url is null or image_url like 'https://coodsyfdhjpqfxanqhwm.supabase.co/storage/v1/object/public/arena-meme-submissions/%');

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('arena-meme-submissions','arena-meme-submissions',true,5242880,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=true,file_size_limit=5242880,allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "Players can upload their own Meme War images" on storage.objects;
create policy "Players can upload their own Meme War images"
on storage.objects for insert to authenticated
with check (
  bucket_id='arena-meme-submissions'
  and (storage.foldername(name))[1]=(select auth.uid())::text
);
-- Public-bucket object GETs work by URL; do not grant object listing access.
drop policy if exists "Public can view Meme War submission images" on storage.objects;
