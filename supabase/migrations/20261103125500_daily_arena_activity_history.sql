create table if not exists public.arena_activity_events (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 event_type text not null check (event_type in ('hot_seat_vote','quickfire_answer','meme_caption','meme_vote')),
 entity_id uuid,
 created_at timestamptz not null default now()
);
create index if not exists arena_activity_events_user_created_idx on public.arena_activity_events(user_id,created_at desc);
create index if not exists arena_activity_events_user_type_created_idx on public.arena_activity_events(user_id,event_type,created_at desc);
alter table public.arena_activity_events enable row level security;
revoke all on public.arena_activity_events from anon,authenticated;
grant select on public.arena_activity_events to authenticated;
drop policy if exists "Players read their own Daily Arena activity" on public.arena_activity_events;
create policy "Players read their own Daily Arena activity" on public.arena_activity_events for select to authenticated
using (user_id=(select auth.uid()));
create or replace function private.log_arena_activity_event()
returns trigger language plpgsql security definer set search_path=pg_catalog,public as $$
declare v_user_id uuid; v_entity_id uuid;
begin
 v_user_id := (to_jsonb(new)->>'user_id')::uuid;
 v_entity_id := nullif(to_jsonb(new)->>tg_argv[1],'')::uuid;
 if auth.uid() is not null and v_user_id=auth.uid() then
  insert into public.arena_activity_events(user_id,event_type,entity_id) values(v_user_id,tg_argv[0],v_entity_id);
 end if;
 return new;
end; $$;
revoke all on function private.log_arena_activity_event() from public,anon,authenticated;
drop trigger if exists arena_log_hot_seat_vote on public.arena_hot_seat_votes;
create trigger arena_log_hot_seat_vote after insert on public.arena_hot_seat_votes for each row
execute function private.log_arena_activity_event('hot_seat_vote','poll_id');
drop trigger if exists arena_log_quickfire_answer on public.arena_quickfire_answers;
create trigger arena_log_quickfire_answer after insert on public.arena_quickfire_answers for each row
execute function private.log_arena_activity_event('quickfire_answer','question_id');
drop trigger if exists arena_log_meme_caption on public.arena_meme_submissions;
create trigger arena_log_meme_caption after insert on public.arena_meme_submissions for each row
execute function private.log_arena_activity_event('meme_caption','challenge_id');
drop trigger if exists arena_log_meme_vote on public.arena_meme_votes;
create trigger arena_log_meme_vote after insert on public.arena_meme_votes for each row
execute function private.log_arena_activity_event('meme_vote','submission_id');
