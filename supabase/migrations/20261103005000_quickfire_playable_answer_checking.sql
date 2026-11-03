-- Quick Fire playable foundation: private answer keys and server-checked answers.
create table if not exists public.arena_quickfire_answers (
  id uuid primary key default gen_random_uuid(),
  question_id uuid not null references public.arena_quickfire_questions(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  selected_option_index smallint not null check (selected_option_index between 0 and 5),
  is_correct boolean not null,
  created_at timestamptz not null default now(),
  unique (question_id, user_id)
);
alter table public.arena_quickfire_answers enable row level security;
drop policy if exists "Players read their own Quick Fire answers" on public.arena_quickfire_answers;
create policy "Players read their own Quick Fire answers"
on public.arena_quickfire_answers for select to authenticated
using (user_id = (select auth.uid()));
revoke all on public.arena_quickfire_answers from anon, authenticated;
grant select on public.arena_quickfire_answers to authenticated;
grant all on public.arena_quickfire_answers to service_role;

create or replace function private.submit_quickfire_answer(
  p_question_id uuid, p_selected_option_index smallint
)
returns table (is_correct boolean, correct_option_index smallint, explanation text, already_answered boolean)
language plpgsql security definer
set search_path = public, private, auth, pg_temp
as $$
declare
  v_user_id uuid := auth.uid();
  v_correct_index smallint;
  v_explanation text;
  v_existing public.arena_quickfire_answers%rowtype;
  v_inserted integer := 0;
begin
  if v_user_id is null then raise exception 'Sign in to play Quick Fire.'; end if;
  if p_selected_option_index < 0 or p_selected_option_index > 5 then raise exception 'Invalid answer choice.'; end if;
  if not exists (
    select 1 from public.arena_quickfire_questions q
    join public.arena_quickfire_sets s on s.id = q.set_id
    where q.id = p_question_id and s.status = 'published'
      and (s.available_from is null or s.available_from <= now())
      and (s.available_until is null or s.available_until > now())
      and p_selected_option_index < jsonb_array_length(q.options)
  ) then raise exception 'This question is not available.'; end if;
  select k.correct_option_index, k.explanation into v_correct_index, v_explanation
  from private.arena_quickfire_answer_keys k where k.question_id = p_question_id;
  if v_correct_index is null then raise exception 'Answer key is not ready for this question.'; end if;
  insert into public.arena_quickfire_answers (question_id, user_id, selected_option_index, is_correct)
  values (p_question_id, v_user_id, p_selected_option_index, p_selected_option_index = v_correct_index)
  on conflict (question_id, user_id) do nothing;
  get diagnostics v_inserted = row_count;
  select * into v_existing from public.arena_quickfire_answers a
  where a.question_id = p_question_id and a.user_id = v_user_id;
  return query select v_existing.is_correct, v_correct_index, v_explanation, v_inserted = 0;
end;
$$;
revoke all on function private.submit_quickfire_answer(uuid, smallint) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.submit_quickfire_answer(uuid, smallint) to authenticated;
create or replace function public.arena_quickfire_submit_answer(p_question_id uuid, p_selected_option_index smallint)
returns table (is_correct boolean, correct_option_index smallint, explanation text, already_answered boolean)
language sql security invoker set search_path = public, private, pg_temp
as $$ select * from private.submit_quickfire_answer(p_question_id, p_selected_option_index); $$;
revoke all on function public.arena_quickfire_submit_answer(uuid, smallint) from public, anon;
grant execute on function public.arena_quickfire_submit_answer(uuid, smallint) to authenticated;

with new_set as (
  insert into public.arena_quickfire_sets (title, description, status)
  select 'Quick Fire: Naija Mix 01',
    'A fast mix of Nigerian culture, everyday knowledge, science and technology.', 'published'
  where not exists (select 1 from public.arena_quickfire_sets where title = 'Quick Fire: Naija Mix 01')
  returning id
), chosen_set as (
  select id from new_set
  union all select id from public.arena_quickfire_sets where title = 'Quick Fire: Naija Mix 01'
  limit 1
), q as (
  insert into public.arena_quickfire_questions (set_id, question_text, options, position, points)
  select chosen_set.id, v.question_text, v.options::jsonb, v.position, 10
  from chosen_set cross join (values
    (1, 'Which Nigerian city is the country’s federal capital?', '["Lagos","Abuja","Kano","Enugu"]'),
    (2, 'What is the official currency of Nigeria?', '["Cedi","Naira","Dalasi","Shilling"]'),
    (3, 'Which planet is known as the Red Planet?', '["Venus","Jupiter","Mars","Mercury"]'),
    (4, 'How many sides does a hexagon have?', '["Five","Six","Seven","Eight"]'),
    (5, 'Which ocean lies to the south of Nigeria?', '["Indian Ocean","Pacific Ocean","Atlantic Ocean","Arctic Ocean"]'),
    (6, 'What does CPU stand for in computing?', '["Central Processing Unit","Computer Power Utility","Core Program Upload","Central Program User"]'),
    (7, 'Which instrument is commonly used to measure temperature?', '["Barometer","Thermometer","Compass","Speedometer"]'),
    (8, 'Which Nigerian writer authored “Things Fall Apart”?', '["Chinua Achebe","Wole Soyinka","Chimamanda Ngozi Adichie","Buchi Emecheta"]'),
    (9, 'What is the process by which green plants make food using sunlight?', '["Evaporation","Photosynthesis","Condensation","Fermentation"]'),
    (10, 'How many states make up the Federal Republic of Nigeria?', '["30","32","36","40"]')
  ) as v(position, question_text, options)
  where not exists (select 1 from public.arena_quickfire_questions old
    where old.set_id = chosen_set.id and old.position = v.position)
  returning id, position
)
insert into private.arena_quickfire_answer_keys (question_id, correct_option_index, explanation)
select q.id, v.correct_index, v.explanation from q join (values
  (1, 1, 'Abuja became Nigeria’s federal capital in 1991.'),
  (2, 1, 'The naira is the official currency of Nigeria.'),
  (3, 2, 'Mars appears reddish because iron minerals in its soil have oxidised.'),
  (4, 1, 'A hexagon has six sides.'),
  (5, 2, 'Nigeria has a coastline along the Gulf of Guinea, part of the Atlantic Ocean.'),
  (6, 0, 'CPU means Central Processing Unit; it carries out instructions in a computer.'),
  (7, 1, 'A thermometer measures temperature.'),
  (8, 0, 'Chinua Achebe wrote the classic novel Things Fall Apart.'),
  (9, 1, 'Photosynthesis uses light energy to help plants make sugars.'),
  (10, 2, 'Nigeria has 36 states, in addition to the Federal Capital Territory.')
) as v(position, correct_index, explanation) using (position)
on conflict (question_id) do nothing;
