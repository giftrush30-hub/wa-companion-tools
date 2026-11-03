create or replace function private.arena_hot_seat_results(p_poll_id uuid)
returns table(option_id uuid, vote_count bigint)
language sql
stable
security definer
set search_path = pg_catalog, public
as $$
  select o.id, count(v.id)::bigint
  from public.arena_hot_seat_options o
  left join public.arena_hot_seat_votes v
    on v.option_id = o.id and v.poll_id = o.poll_id
  join public.arena_hot_seat_polls p on p.id = o.poll_id
  where o.poll_id = p_poll_id
    and p.status in ('published','closed')
    and auth.uid() is not null
  group by o.id, o.position
  order by o.position
$$;
revoke all on function private.arena_hot_seat_results(uuid) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.arena_hot_seat_results(uuid) to authenticated;

create or replace function public.arena_hot_seat_results(p_poll_id uuid)
returns table(option_id uuid, vote_count bigint)
language sql
stable
security invoker
set search_path = public, private, pg_catalog
as $$ select * from private.arena_hot_seat_results(p_poll_id); $$;
revoke all on function public.arena_hot_seat_results(uuid) from public, anon;
grant execute on function public.arena_hot_seat_results(uuid) to authenticated;
