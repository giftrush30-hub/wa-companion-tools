-- Admin-only Meme War moderation. Uses trusted app_metadata role, never user_metadata.
drop policy if exists "Meme War admins can review all submissions" on public.arena_meme_submissions;
create policy "Meme War admins can review all submissions"
  on public.arena_meme_submissions for select to authenticated
  using ((select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin');

drop policy if exists "Meme War admins can moderate submissions" on public.arena_meme_submissions;
create policy "Meme War admins can moderate submissions"
  on public.arena_meme_submissions for update to authenticated
  using ((select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin')
  with check (
    (select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin'
    and status in ('approved','rejected','removed')
  );

revoke update on public.arena_meme_submissions from authenticated;
grant update (status) on public.arena_meme_submissions to authenticated;
