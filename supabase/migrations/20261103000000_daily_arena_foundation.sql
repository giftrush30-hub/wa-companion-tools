-- Daily Arena foundation: content, submissions, voting and reports.
-- Moderation and answer-key writes are server-only. Never ship a service-role key in Android.

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to service_role;

create table public.arena_hot_seat_polls (
  id uuid primary key default gen_random_uuid(),
  question text not null check (char_length(question) between 5 and 500),
  status text not null default 'draft' check (status in ('draft','published','closed')),
  starts_at timestamptz, ends_at timestamptz,
  created_at timestamptz not null default now(),
  check (ends_at is null or starts_at is null or ends_at > starts_at)
);
create table public.arena_hot_seat_options (
  id uuid primary key default gen_random_uuid(),
  poll_id uuid not null references public.arena_hot_seat_polls(id) on delete cascade,
  option_text text not null check (char_length(option_text) between 1 and 180),
  position smallint not null check (position between 1 and 8),
  unique (poll_id, position), unique (poll_id, id)
);
create table public.arena_hot_seat_votes (
  id uuid primary key default gen_random_uuid(),
  poll_id uuid not null references public.arena_hot_seat_polls(id) on delete cascade,
  option_id uuid not null,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  foreign key (poll_id, option_id) references public.arena_hot_seat_options(poll_id, id) on delete cascade,
  unique (poll_id, user_id)
);
create index arena_hot_seat_votes_poll_idx on public.arena_hot_seat_votes(poll_id);

create table public.arena_quickfire_sets (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 2 and 120),
  description text,
  status text not null default 'draft' check (status in ('draft','published','closed')),
  available_from timestamptz, available_until timestamptz,
  created_at timestamptz not null default now(),
  check (available_until is null or available_from is null or available_until > available_from)
);
create table public.arena_quickfire_questions (
  id uuid primary key default gen_random_uuid(),
  set_id uuid not null references public.arena_quickfire_sets(id) on delete cascade,
  question_text text not null check (char_length(question_text) between 5 and 1000),
  options jsonb not null check (jsonb_typeof(options) = 'array' and jsonb_array_length(options) between 2 and 6),
  position smallint not null check (position between 1 and 100),
  points integer not null default 1 check (points between 1 and 100),
  unique (set_id, position)
);
create table private.arena_quickfire_answer_keys (
  question_id uuid primary key references public.arena_quickfire_questions(id) on delete cascade,
  correct_option_index smallint not null check (correct_option_index between 0 and 5),
  explanation text,
  updated_at timestamptz not null default now()
);
alter table private.arena_quickfire_answer_keys enable row level security;
grant select, insert, update, delete on private.arena_quickfire_answer_keys to service_role;
create policy "Server role manages Quick Fire answer keys"
on private.arena_quickfire_answer_keys for all to service_role
using (true) with check (true);

create table public.arena_meme_challenges (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 2 and 120),
  instructions text not null default 'Write a caption for this challenge.',
  template_image_url text,
  status text not null default 'draft' check (status in ('draft','published','closed')),
  starts_at timestamptz, ends_at timestamptz,
  created_at timestamptz not null default now(),
  check (template_image_url is null or template_image_url ~ '^https://'),
  check (ends_at is null or starts_at is null or ends_at > starts_at)
);
create table public.arena_meme_submissions (
  id uuid primary key default gen_random_uuid(),
  challenge_id uuid not null references public.arena_meme_challenges(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  caption text not null check (char_length(caption) between 1 and 500),
  status text not null default 'pending' check (status in ('pending','approved','rejected','removed')),
  created_at timestamptz not null default now()
);
create index arena_meme_submissions_challenge_status_idx on public.arena_meme_submissions(challenge_id, status);
create table public.arena_meme_votes (
  id uuid primary key default gen_random_uuid(),
  submission_id uuid not null references public.arena_meme_submissions(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (submission_id, user_id)
);
create index arena_meme_votes_submission_idx on public.arena_meme_votes(submission_id);

create table public.arena_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users(id) on delete cascade,
  target_type text not null check (target_type in ('hot_seat_poll','meme_challenge','meme_submission','quickfire_set')),
  target_id uuid not null,
  reason text not null check (reason in ('harassment','hate','sexual_content','violence','spam','privacy','other')),
  details text check (details is null or char_length(details) <= 1000),
  status text not null default 'open' check (status in ('open','reviewing','resolved','dismissed')),
  created_at timestamptz not null default now()
);
create index arena_reports_status_created_idx on public.arena_reports(status, created_at);

alter table public.arena_hot_seat_polls enable row level security;
alter table public.arena_hot_seat_options enable row level security;
alter table public.arena_hot_seat_votes enable row level security;
alter table public.arena_quickfire_sets enable row level security;
alter table public.arena_quickfire_questions enable row level security;
alter table public.arena_meme_challenges enable row level security;
alter table public.arena_meme_submissions enable row level security;
alter table public.arena_meme_votes enable row level security;
alter table public.arena_reports enable row level security;

create policy "Published Hot Seat polls are readable" on public.arena_hot_seat_polls
for select to anon, authenticated using (status in ('published','closed'));
create policy "Options for published polls are readable" on public.arena_hot_seat_options
for select to anon, authenticated using (exists (
  select 1 from public.arena_hot_seat_polls p where p.id = poll_id and p.status in ('published','closed')
));
create policy "Signed-in players can vote once in an active poll" on public.arena_hot_seat_votes
for insert to authenticated with check (
  user_id = (select auth.uid()) and exists (
    select 1 from public.arena_hot_seat_polls p where p.id = poll_id and p.status = 'published'
    and (p.starts_at is null or p.starts_at <= now()) and (p.ends_at is null or p.ends_at > now())
  )
);
create policy "Published Quick Fire sets are readable" on public.arena_quickfire_sets
for select to anon, authenticated using (status in ('published','closed'));
create policy "Questions in published Quick Fire sets are readable" on public.arena_quickfire_questions
for select to anon, authenticated using (exists (
  select 1 from public.arena_quickfire_sets s where s.id = set_id and s.status in ('published','closed')
));
create policy "Published Meme War challenges are readable" on public.arena_meme_challenges
for select to anon, authenticated using (status in ('published','closed'));
create policy "Players can read approved captions and their own submissions" on public.arena_meme_submissions
for select to authenticated using (status = 'approved' or user_id = (select auth.uid()));
create policy "Players can submit captions for active challenges" on public.arena_meme_submissions
for insert to authenticated with check (
  user_id = (select auth.uid()) and status = 'pending' and exists (
    select 1 from public.arena_meme_challenges c where c.id = challenge_id and c.status = 'published'
    and (c.starts_at is null or c.starts_at <= now()) and (c.ends_at is null or c.ends_at > now())
  )
);
create policy "Players can vote once on approved captions" on public.arena_meme_votes
for insert to authenticated with check (
  user_id = (select auth.uid()) and exists (
    select 1 from public.arena_meme_submissions s where s.id = submission_id
    and s.status = 'approved' and s.user_id <> (select auth.uid())
  )
);
create policy "Reporters can read their own reports" on public.arena_reports
for select to authenticated using (reporter_id = (select auth.uid()));
create policy "Signed-in players can submit reports" on public.arena_reports
for insert to authenticated with check (reporter_id = (select auth.uid()) and status = 'open');

grant select on public.arena_hot_seat_polls, public.arena_hot_seat_options,
  public.arena_quickfire_sets, public.arena_quickfire_questions, public.arena_meme_challenges
to anon, authenticated;
grant select on public.arena_meme_submissions to authenticated;
grant insert on public.arena_hot_seat_votes, public.arena_meme_submissions,
  public.arena_meme_votes, public.arena_reports to authenticated;
grant select on public.arena_reports to authenticated;
grant all on public.arena_hot_seat_polls, public.arena_hot_seat_options, public.arena_hot_seat_votes,
  public.arena_quickfire_sets, public.arena_quickfire_questions, public.arena_meme_challenges,
  public.arena_meme_submissions, public.arena_meme_votes, public.arena_reports to service_role;
