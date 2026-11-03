drop policy if exists "Meme War admins can review all submissions" on public.arena_meme_submissions;
drop policy if exists "Players can read approved captions and their own submissions" on public.arena_meme_submissions;
create policy "Players and admins can read Meme War submissions"
on public.arena_meme_submissions for select to authenticated
using (
 status='approved'
 or user_id=(select auth.uid())
 or ((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin'
);
drop policy if exists "Arena admins can review reports" on public.arena_reports;
drop policy if exists "Reporters can read their own reports" on public.arena_reports;
create policy "Reporters and admins can read reports"
on public.arena_reports for select to authenticated
using (
 reporter_id=(select auth.uid())
 or ((select auth.jwt()) -> 'app_metadata' ->> 'role')='admin'
);
