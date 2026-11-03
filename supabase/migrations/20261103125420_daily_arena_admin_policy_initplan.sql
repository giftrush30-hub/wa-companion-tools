drop policy if exists "Meme War admins can review all submissions" on public.arena_meme_submissions;
create policy "Meme War admins can review all submissions" on public.arena_meme_submissions for select to authenticated
using (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin');
drop policy if exists "Meme War admins can moderate submissions" on public.arena_meme_submissions;
create policy "Meme War admins can moderate submissions" on public.arena_meme_submissions for update to authenticated
using (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin')
with check (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin' and status in ('approved','rejected','removed'));
drop policy if exists "Arena admins can review reports" on public.arena_reports;
create policy "Arena admins can review reports" on public.arena_reports for select to authenticated
using (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin');
drop policy if exists "Arena admins can update report status" on public.arena_reports;
create policy "Arena admins can update report status" on public.arena_reports for update to authenticated
using (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin')
with check (((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin' and status in ('open','reviewing','resolved','dismissed'));