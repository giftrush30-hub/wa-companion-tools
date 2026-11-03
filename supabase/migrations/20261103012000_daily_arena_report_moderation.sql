-- Admin-only review queue for Daily Arena reports.
drop policy if exists "Arena admins can review reports" on public.arena_reports;
create policy "Arena admins can review reports"
  on public.arena_reports for select to authenticated
  using ((select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin');

drop policy if exists "Arena admins can update report status" on public.arena_reports;
create policy "Arena admins can update report status"
  on public.arena_reports for update to authenticated
  using ((select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin')
  with check (
    (select auth.jwt() -> 'app_metadata' ->> 'role') = 'admin'
    and status in ('open','reviewing','resolved','dismissed')
  );

grant select on public.arena_reports to authenticated;
revoke update on public.arena_reports from authenticated;
grant update (status) on public.arena_reports to authenticated;
