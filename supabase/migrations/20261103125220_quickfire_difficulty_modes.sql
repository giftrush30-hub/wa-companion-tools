alter table public.arena_quickfire_questions add column if not exists difficulty text not null default 'standard';
alter table public.arena_quickfire_questions drop constraint if exists arena_quickfire_questions_difficulty_check;
alter table public.arena_quickfire_questions add constraint arena_quickfire_questions_difficulty_check
check (difficulty in ('easy','standard','expert'));
update public.arena_quickfire_questions set difficulty=case
 when position between 1 and 5 then 'easy'
 when position between 6 and 10 then 'standard'
 else 'expert' end;
create index if not exists arena_quickfire_questions_set_difficulty_idx
on public.arena_quickfire_questions(set_id,difficulty,position);
