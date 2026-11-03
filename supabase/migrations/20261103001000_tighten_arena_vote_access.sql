-- Tighten client access to raw votes and index foreign-key lookup paths.
revoke all on public.arena_hot_seat_votes, public.arena_meme_votes from anon, authenticated;
grant insert on public.arena_hot_seat_votes, public.arena_meme_votes to authenticated;
grant all on public.arena_hot_seat_votes, public.arena_meme_votes to service_role;

create index if not exists arena_hot_seat_votes_poll_option_idx
  on public.arena_hot_seat_votes(poll_id, option_id);
create index if not exists arena_hot_seat_votes_user_idx
  on public.arena_hot_seat_votes(user_id);
create index if not exists arena_meme_submissions_user_idx
  on public.arena_meme_submissions(user_id);
create index if not exists arena_meme_votes_user_idx
  on public.arena_meme_votes(user_id);
create index if not exists arena_reports_reporter_idx
  on public.arena_reports(reporter_id);
