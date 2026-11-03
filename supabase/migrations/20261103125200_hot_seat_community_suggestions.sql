create table if not exists public.arena_hot_seat_suggestions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  question text not null check (char_length(btrim(question)) between 12 and 180),
  topic text not null default 'General' check (char_length(topic) between 2 and 40),
  options jsonb not null check (jsonb_typeof(options)='array' and jsonb_array_length(options) between 2 and 4),
  status text not null default 'pending' check (status in ('pending','approved','rejected','removed')),
  created_at timestamptz not null default now()
);
create index if not exists arena_hot_seat_suggestions_status_created_idx on public.arena_hot_seat_suggestions(status,created_at desc);
create table if not exists public.arena_hot_seat_suggestion_votes (
  id uuid primary key default gen_random_uuid(),
  suggestion_id uuid not null references public.arena_hot_seat_suggestions(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint arena_hot_seat_suggestion_one_vote unique(suggestion_id,user_id)
);
alter table public.arena_hot_seat_suggestions enable row level security;
alter table public.arena_hot_seat_suggestion_votes enable row level security;
revoke all on public.arena_hot_seat_suggestions,public.arena_hot_seat_suggestion_votes from anon,authenticated;
grant select,insert on public.arena_hot_seat_suggestions to authenticated;
grant update(status) on public.arena_hot_seat_suggestions to authenticated;
grant select,insert on public.arena_hot_seat_suggestion_votes to authenticated;
drop policy if exists "Players read approved or own Hot Seat suggestions" on public.arena_hot_seat_suggestions;
create policy "Players read approved or own Hot Seat suggestions" on public.arena_hot_seat_suggestions for select to authenticated
using (status='approved' or user_id=(select auth.uid()) or (select auth.jwt()->'app_metadata'->>'role')='admin');
drop policy if exists "Players submit their own Hot Seat suggestions" on public.arena_hot_seat_suggestions;
create policy "Players submit their own Hot Seat suggestions" on public.arena_hot_seat_suggestions for insert to authenticated
with check (user_id=(select auth.uid()) and status='pending');
drop policy if exists "Admins moderate Hot Seat suggestions" on public.arena_hot_seat_suggestions;
create policy "Admins moderate Hot Seat suggestions" on public.arena_hot_seat_suggestions for update to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='admin')
with check ((select auth.jwt()->'app_metadata'->>'role')='admin' and status in ('approved','rejected','removed'));
drop policy if exists "Players read only their Hot Seat suggestion votes" on public.arena_hot_seat_suggestion_votes;
create policy "Players read only their Hot Seat suggestion votes" on public.arena_hot_seat_suggestion_votes for select to authenticated
using (user_id=(select auth.uid()));
drop policy if exists "Players vote as themselves on Hot Seat suggestions" on public.arena_hot_seat_suggestion_votes;
create policy "Players vote as themselves on Hot Seat suggestions" on public.arena_hot_seat_suggestion_votes for insert to authenticated
with check (user_id=(select auth.uid()) and exists(select 1 from public.arena_hot_seat_suggestions s where s.id=suggestion_id and s.status='approved'));
create or replace function private.enforce_hot_seat_suggestion_rate_limit()
returns trigger language plpgsql security definer set search_path=pg_catalog,public as $$
begin
  if auth.uid() is null or new.user_id <> auth.uid() then raise exception 'Authenticated user mismatch'; end if;
  if (select count(*) from public.arena_hot_seat_suggestions where user_id=auth.uid() and created_at > now()-interval '24 hours') >= 3 then
    raise exception 'You have reached the limit of 3 Hot Seat suggestions per 24 hours.';
  end if;
  if jsonb_typeof(new.options) <> 'array' or jsonb_array_length(new.options) not between 2 and 4
     or exists(select 1 from jsonb_array_elements_text(new.options) opt where char_length(btrim(opt)) not between 1 and 70) then
    raise exception 'Provide 2 to 4 answer options, each 1 to 70 characters.';
  end if;
  return new;
end; $$;
revoke all on function private.enforce_hot_seat_suggestion_rate_limit() from public,anon,authenticated;
drop trigger if exists enforce_hot_seat_suggestion_rate_limit on public.arena_hot_seat_suggestions;
create trigger enforce_hot_seat_suggestion_rate_limit before insert on public.arena_hot_seat_suggestions
for each row execute function private.enforce_hot_seat_suggestion_rate_limit();
create or replace function private.arena_hot_seat_suggestion_vote_counts(p_ids uuid[])
returns table(suggestion_id uuid,vote_count bigint) language sql stable security definer
set search_path=pg_catalog,public as $$
 select s.id,count(v.id)::bigint from public.arena_hot_seat_suggestions s
 left join public.arena_hot_seat_suggestion_votes v on v.suggestion_id=s.id
 where s.id=any(p_ids) and s.status='approved' and auth.uid() is not null group by s.id
$$;
revoke all on function private.arena_hot_seat_suggestion_vote_counts(uuid[]) from public,anon;
grant usage on schema private to authenticated;
grant execute on function private.arena_hot_seat_suggestion_vote_counts(uuid[]) to authenticated;
create or replace function public.arena_hot_seat_suggestion_vote_counts(p_ids uuid[])
returns table(suggestion_id uuid,vote_count bigint) language sql stable security invoker
set search_path=public,private,pg_catalog as $$ select * from private.arena_hot_seat_suggestion_vote_counts(p_ids); $$;
revoke all on function public.arena_hot_seat_suggestion_vote_counts(uuid[]) from public,anon;
grant execute on function public.arena_hot_seat_suggestion_vote_counts(uuid[]) to authenticated;
