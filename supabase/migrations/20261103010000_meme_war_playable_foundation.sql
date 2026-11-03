-- Meme War first playable release: one caption per player, private vote identities, safe aggregate counts.
create unique index if not exists arena_meme_one_caption_per_player
  on public.arena_meme_submissions (challenge_id, user_id);

drop policy if exists "Players can read their own Meme War votes" on public.arena_meme_votes;
create policy "Players can read their own Meme War votes"
  on public.arena_meme_votes for select to authenticated
  using (user_id = (select auth.uid()));
grant select on public.arena_meme_votes to authenticated;

create or replace function private.meme_war_vote_counts(p_challenge_id uuid)
returns table (submission_id uuid, vote_count bigint)
language plpgsql security definer
set search_path = public, private, auth, pg_temp
as $$
begin
  if auth.uid() is null then raise exception 'Sign in to view Meme War votes.'; end if;
  if not exists (select 1 from public.arena_meme_challenges c
    where c.id = p_challenge_id and c.status in ('published','closed')) then
    raise exception 'Meme War challenge not found.';
  end if;
  return query select s.id, count(v.id)::bigint
    from public.arena_meme_submissions s
    left join public.arena_meme_votes v on v.submission_id = s.id
    where s.challenge_id = p_challenge_id and s.status = 'approved'
    group by s.id;
end;
$$;
revoke all on function private.meme_war_vote_counts(uuid) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.meme_war_vote_counts(uuid) to authenticated;

create or replace function public.arena_meme_vote_counts(p_challenge_id uuid)
returns table (submission_id uuid, vote_count bigint)
language sql security invoker
set search_path = public, private, pg_temp
as $$ select * from private.meme_war_vote_counts(p_challenge_id); $$;
revoke all on function public.arena_meme_vote_counts(uuid) from public, anon;
grant execute on function public.arena_meme_vote_counts(uuid) to authenticated;

insert into public.arena_meme_challenges
  (title, instructions, template_image_url, status, starts_at, ends_at)
select 'Naija Everyday Moments',
  'Write one original, funny caption for this situation: You finally buy data, open your favourite app, and the network starts moving like it is on holiday. Keep it friendly, original and under 500 characters. Submissions are reviewed before they appear for voting.',
  null, 'published', now(), now() + interval '14 days'
where not exists (select 1 from public.arena_meme_challenges
  where title = 'Naija Everyday Moments' and status = 'published'
    and (ends_at is null or ends_at > now()));
