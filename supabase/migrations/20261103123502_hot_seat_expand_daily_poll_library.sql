alter table public.arena_hot_seat_polls
  drop constraint if exists arena_hot_seat_polls_poll_type_check;
alter table public.arena_hot_seat_polls
  add constraint arena_hot_seat_polls_poll_type_check
  check (poll_type in ('poll','this_or_that','hot_take','rating'));

-- The 15 additional Hot Seat polls are seeded in this migration's data section.
-- Each poll has four options except the two-choice This-or-That question.
