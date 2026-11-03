alter table public.arena_hot_seat_polls
  drop constraint if exists arena_hot_seat_polls_poll_type_check;
alter table public.arena_hot_seat_polls
  add constraint arena_hot_seat_polls_poll_type_check
  check (poll_type in ('poll','this_or_that','hot_take','rating'));

with data(question,topic,poll_type,options) as (values
('If you could instantly master one skill, what would you choose?','Life & Growth','poll','["Public speaking","Making money online","A creative skill","A practical trade"]'::jsonb),
('Which Nigerian city would you most like to explore for a weekend?','Naija Life','poll','["Lagos","Abuja","Calabar","Kano"]'::jsonb),
('What makes you trust a new business most?','Business & Money','poll','["Honest reviews","Clear prices","Good customer service","Recommendations from people I know"]'::jsonb),
('Which would improve your daily life the most right now?','Life & Growth','poll','["More time","More money","Better health habits","Less stress"]'::jsonb),
('Which kind of content do you stop scrolling for?','Entertainment','poll','["Comedy","Useful tutorials","Real-life stories","Music and dance"]'::jsonb),
('Which Nigerian snack would you pick right now?','Naija Life','poll','["Puff-puff","Akara","Boli","Chin chin"]'::jsonb),
('What is the best way to settle a misunderstanding?','Relationships','poll','["Talk calmly in private","Give each other time","Use a trusted mediator","Write a thoughtful message"]'::jsonb),
('What should a great friend always bring?','Relationships','poll','["Honesty","Loyalty","Encouragement","A good sense of humour"]'::jsonb),
('Which technology skill should more young people learn?','Tech & Future','poll','["Coding","Digital design","Cybersecurity","Video editing"]'::jsonb),
('What makes an online community enjoyable?','Community','poll','["Funny conversations","Helpful people","Fair competitions","Respectful debates"]'::jsonb),
('If you had a free day tomorrow, what would you do?','Lifestyle','poll','["Rest and recharge","Learn something new","Visit somewhere new","Spend time with loved ones"]'::jsonb),
('Which quality matters most in a leader?','Community','poll','["Integrity","Good judgement","Empathy","Courage"]'::jsonb),
('What is your preferred way to learn something new?','Learning','poll','["Short videos","Step-by-step guides","Practice and challenges","Learning with friends"]'::jsonb),
('Which would you rather win?','This or That','this_or_that','["A fully paid holiday","A new phone"]'::jsonb),
('What makes a day feel successful?','Hot Take','hot_take','["Making progress","Helping someone","Learning something","Finding peace"]'::jsonb)
), inserted as (
  insert into public.arena_hot_seat_polls(question,topic,poll_type,status,starts_at)
  select d.question,d.topic,d.poll_type,'published',now()
  from data d
  where not exists(select 1 from public.arena_hot_seat_polls p where p.question=d.question)
  returning id,question
)
insert into public.arena_hot_seat_options(poll_id,option_text,position)
select i.id,opt.value,opt.ordinality::smallint
from inserted i
join data d using(question)
cross join lateral jsonb_array_elements_text(d.options) with ordinality as opt(value,ordinality);
