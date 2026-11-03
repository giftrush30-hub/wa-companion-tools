create index if not exists arena_hot_seat_suggestions_user_id_idx on public.arena_hot_seat_suggestions(user_id);
create index if not exists arena_hot_seat_suggestion_votes_user_id_idx on public.arena_hot_seat_suggestion_votes(user_id);
create index if not exists arena_quickfire_answers_user_id_idx on public.arena_quickfire_answers(user_id);
