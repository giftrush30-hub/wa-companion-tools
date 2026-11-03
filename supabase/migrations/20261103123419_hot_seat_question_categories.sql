alter table public.arena_hot_seat_polls
  add column if not exists topic text not null default 'General';
alter table public.arena_hot_seat_polls
  add column if not exists poll_type text not null default 'poll';
