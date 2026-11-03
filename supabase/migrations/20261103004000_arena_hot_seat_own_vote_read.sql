grant select on public.arena_hot_seat_votes to authenticated;
create policy "Players can read only their own Hot Seat vote"
on public.arena_hot_seat_votes
for select
to authenticated
using ((select auth.uid()) = user_id);
