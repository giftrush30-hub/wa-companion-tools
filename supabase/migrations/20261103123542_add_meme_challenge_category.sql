alter table public.arena_meme_challenges
  add column if not exists category text not null default 'Everyday Naija';
