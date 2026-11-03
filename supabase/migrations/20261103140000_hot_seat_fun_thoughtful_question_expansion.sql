-- Add a fresh, varied Hot Seat content batch: funny dilemmas, Naija everyday life and thoughtful choices.
-- Idempotent by question text so reapplying does not create duplicate polls.
with seed(question, topic, poll_type, options) as (
  values
  ('Your friend says “I am 5 minutes away” but has not entered the bathroom yet. What do you do?','Naija Banter','this_or_that',array['Start your own journey','Call and ask for the truth','Wait patiently like a hopeful citizen','Send “no wahala” and move on']),
  ('You mistakenly transfer ₦10,000 to the wrong person. What is your first move?','Money & Real Life','hot_take',array['Call politely immediately','Contact your bank through official channels','Send a calm message with proof','Panic first, then become responsible']),
  ('Which small luxury feels like a major achievement after a stressful week?','Lifestyle','poll',array['A full night of sleep','Food arriving hot and correct','A surprise alert','A day with no unnecessary calls']),
  ('If your phone could expose only ONE habit, which one would embarrass you most?','Funny & Honest','poll',array['Checking the same app repeatedly','Taking screenshots I never revisit','Opening the fridge without a plan','Saying “I am coming” while still seated']),
  ('A person apologises but repeats the same behaviour. What matters most next?','Relationships','hot_take',array['The apology','Consistent changed behaviour','Time and boundaries','A direct conversation']),
  ('You have one free hour and no money to spend. Which option wins?','Life & Growth','poll',array['Learn something useful','Call someone I miss','Take a proper rest','Create something with my phone']),
  ('Which is the most convincing sign that someone is truly dependable?','Relationships','poll',array['They keep small promises','They show up when it matters','They tell you the truth kindly','They take responsibility after mistakes']),
  ('Your data finishes exactly when the video reaches the interesting part. What is the most Nigerian reaction?','Naija Banter','poll',array['Replay the last 10 seconds in memory','Switch to another network','Start negotiating with the universe','Save the remaining battery and go offline']),
  ('If everyone received the same salary, what would become the biggest status symbol?','Society & Ideas','hot_take',array['Free time','Peace of mind','Useful skills','A genuinely reliable support system']),
  ('You find a wallet with cash and an ID card. What should guide your decision?','Character','hot_take',array['Return it through a safe verified route','Hand it to a trusted official','Contact the owner without exposing details','Make sure the owner is identified before any handover']),
  ('Which one would you rather have for the next 30 days?','This or That','this_or_that',array['Unlimited free data','Unlimited uninterrupted sleep']),
  ('A group project has one person doing all the work. What is the fairest response?','Teamwork','poll',array['Agree clear tasks and deadlines','Talk privately to the group','Ask the group to redistribute the work','Document contributions honestly']),
  ('Which statement deserves more attention in everyday life?','Big Questions','poll',array['Being busy is not the same as making progress','A confident answer can still be wrong','Kindness and boundaries can coexist','Changing your mind can be a sign of growth']),
  ('You are given ₦50,000 to improve your future, not to show off. What do you prioritise?','Money & Business','poll',array['A practical skill or course','A small tested business idea','Tools that help me earn','Savings for an important need']),
  ('Your alarm rings and your dream was more interesting than reality. What happens?','Funny & Honest','poll',array['Five more minutes becomes an hour','I try to continue the dream','I get up and blame the alarm','I negotiate with Monday']),
  ('Which online habit would improve the internet for everybody?','Digital Life','poll',array['Verify before forwarding','Disagree without insults','Respect people’s privacy','Credit original creators']),
  ('You can ask your future self one question and get an honest answer. What do you ask?','Thought Provoker','poll',array['What should I stop delaying?','Which relationship should I nurture?','What skill will matter most?','What am I worrying about unnecessarily?']),
  ('Someone offers you a shortcut that feels dishonest but promises quick success. What is the strongest reason to refuse?','Character','hot_take',array['It can harm other people','The consequences can outlast the reward','Trust is difficult to rebuild','Success should be something I can stand behind']),
  ('Which everyday sound deserves its own national anthem?','Naija Banter','poll',array['Food sizzling in the kitchen','The generator finally starting','A payment alert','Rain beginning when you are indoors']),
  ('You and your friend disagree strongly. What is the best way to keep the friendship healthy?','Relationships','poll',array['Listen before replying','Separate the person from the opinion','Admit when either person is wrong','Agree on boundaries and move forward'])
),
inserted as (
  insert into public.arena_hot_seat_polls(question, topic, poll_type, status, starts_at)
  select s.question, s.topic, s.poll_type, 'published', now()
  from seed s
  where not exists (
    select 1 from public.arena_hot_seat_polls p
    where lower(btrim(p.question)) = lower(btrim(s.question))
  )
  returning id, question
)
insert into public.arena_hot_seat_options(poll_id, option_text, position)
select i.id, option_text, option_position::smallint
from inserted i
join seed s on s.question = i.question
cross join lateral unnest(s.options) with ordinality as o(option_text, option_position);
