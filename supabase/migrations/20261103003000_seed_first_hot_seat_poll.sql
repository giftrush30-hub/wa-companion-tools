-- Curated starter question for the first Daily Arena Hot Seat.
-- Seed only when there is no published poll; never create duplicate live polls.
do $$
declare p_id uuid;
begin
  if not exists (select 1 from public.arena_hot_seat_polls where status='published') then
    insert into public.arena_hot_seat_polls(question,status,starts_at)
    values ('Which skill would make the biggest positive difference in your life this year?','published',now())
    returning id into p_id;
    insert into public.arena_hot_seat_options(poll_id,option_text,position) values
      (p_id,'Learning a digital or technical skill',1),
      (p_id,'Improving my education',2),
      (p_id,'Starting or growing a business',3),
      (p_id,'Building healthier habits',4);
  end if;
end $$;
