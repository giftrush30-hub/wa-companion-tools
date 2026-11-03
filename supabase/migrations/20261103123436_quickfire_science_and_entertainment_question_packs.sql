insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: Science & The World','Science, nature, geography and useful facts.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Science & The World');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'Which gas do plants absorb during photosynthesis?','["Oxygen","Carbon dioxide","Nitrogen","Helium"]'::jsonb,1,'Plants use carbon dioxide, water and light to make sugars.'),
(2,'What is the closest star to Earth?','["Sirius","The Sun","Polaris","Alpha Centauri"]'::jsonb,1,'The Sun is Earth''s nearest star.'),
(3,'At sea level, water boils at approximately what temperature?','["50°C","75°C","100°C","150°C"]'::jsonb,2,'Pure water boils at about 100°C at standard atmospheric pressure.'),
(4,'Which organ pumps blood around the human body?','["Liver","Heart","Lung","Kidney"]'::jsonb,1,'The heart pumps blood through the circulatory system.'),
(5,'What is the largest planet in our solar system?','["Earth","Saturn","Jupiter","Neptune"]'::jsonb,2,'Jupiter is the largest planet in the solar system.'),
(6,'Which continent contains the Sahara Desert?','["Asia","Africa","Australia","Europe"]'::jsonb,1,'The Sahara is in North Africa.'),
(7,'What force pulls objects toward Earth?','["Magnetism","Gravity","Friction","Electricity"]'::jsonb,1,'Gravity attracts objects with mass toward one another.'),
(8,'Which part of a plant usually absorbs water from soil?','["Flower","Roots","Fruit","Leaves"]'::jsonb,1,'Roots absorb water and minerals from the soil.'),
(9,'What is the chemical symbol for gold?','["Ag","Gd","Au","Go"]'::jsonb,2,'Gold''s chemical symbol is Au, from the Latin aurum.'),
(10,'Which animal is the largest living mammal?','["African elephant","Blue whale","Giraffe","Hippopotamus"]'::jsonb,1,'The blue whale is the largest living animal known.'),
(11,'How many bones are usually in an adult human body?','["106","206","306","406"]'::jsonb,1,'Most adults have 206 bones.'),
(12,'Which planet is famous for its prominent rings?','["Mars","Venus","Saturn","Mercury"]'::jsonb,2,'Saturn''s bright ring system is especially prominent.'),
(13,'What is the change from liquid water to water vapour called?','["Freezing","Evaporation","Melting","Condensation"]'::jsonb,1,'Evaporation changes a liquid into a gas.'),
(14,'Which instrument measures atmospheric pressure?','["Thermometer","Barometer","Hygrometer","Compass"]'::jsonb,1,'A barometer measures atmospheric pressure.'),
(15,'What is the hardest naturally occurring mineral?','["Quartz","Diamond","Topaz","Iron"]'::jsonb,1,'Diamond is rated 10 on the Mohs hardness scale.')
), chosen_set as (
 select id from public.arena_quickfire_sets where title='Quick Fire: Science & The World' limit 1
), inserted as (
 insert into public.arena_quickfire_questions(set_id,question_text,options,position,points)
 select chosen_set.id,q.question_text,q.options,q.position,10
 from q cross join chosen_set
 where not exists(select 1 from public.arena_quickfire_questions old
   where old.set_id=chosen_set.id and old.question_text=q.question_text)
 returning id,position
)
insert into private.arena_quickfire_answer_keys(question_id,correct_option_index,explanation)
select inserted.id,q.correct_index,q.explanation
from inserted join q using(position)
on conflict(question_id) do nothing;

insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: Entertainment & Sport','Music, films, sport and pop-culture knowledge.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Entertainment & Sport');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'How many players from one football team are normally on the pitch at kick-off?','["9","10","11","12"]'::jsonb,2,'A standard football team fields eleven players.'),
(2,'In tennis, what score comes after 30–30?','["40–30","Love","Advantage","Game"]'::jsonb,0,'The next point makes the score 40–30.'),
(3,'Which instrument has black and white keys?','["Violin","Piano","Trumpet","Drum"]'::jsonb,1,'A piano uses a keyboard of black and white keys.'),
(4,'Which award is presented by the Recording Academy?','["Oscar","Grammy","BAFTA","Tony"]'::jsonb,1,'The Grammy Awards recognise achievements in music.'),
(5,'How many rings appear on the Olympic symbol?','["Four","Five","Six","Seven"]'::jsonb,1,'The Olympic symbol has five interlaced rings.'),
(6,'Which sport uses a shuttlecock?','["Badminton","Basketball","Rugby","Golf"]'::jsonb,0,'Badminton is played with a shuttlecock.'),
(7,'In basketball, how many points is a shot beyond the three-point line worth?','["1","2","3","4"]'::jsonb,2,'A successful shot beyond the arc scores three points.'),
(8,'Which film-making role guides the actors'' performances?','["Director","Accountant","Projectionist","Ticket seller"]'::jsonb,0,'The director guides the creative and performance choices of a film.'),
(9,'What card does a football referee use to give a caution?','["Blue card","Yellow card","Green card","White card"]'::jsonb,1,'A yellow card is a caution.'),
(10,'Which music family does the saxophone belong to?','["String","Woodwind","Keyboard","Percussion"]'::jsonb,1,'The saxophone is classified as a woodwind instrument.'),
(11,'How long is a standard senior football match, excluding added time?','["60 minutes","80 minutes","90 minutes","100 minutes"]'::jsonb,2,'A standard match has two 45-minute halves.'),
(12,'Which sport is associated with Wimbledon?','["Cricket","Tennis","Hockey","Swimming"]'::jsonb,1,'Wimbledon is one of tennis''s four Grand Slam tournaments.'),
(13,'What is the term for three goals scored by one player in a match?','["Hat-trick","Triple play","Clean sheet","Ace"]'::jsonb,0,'A hat-trick means one player scores three times.'),
(14,'Which device is commonly used to capture professional video and photographs?','["Camera","Router","Printer","Projector"]'::jsonb,0,'A camera captures still images and video.'),
(15,'What does a DJ commonly mix?','["Paint colours","Audio tracks","Cooking oils","Football teams"]'::jsonb,1,'A DJ blends audio tracks to create a continuous music experience.')
), chosen_set as (
 select id from public.arena_quickfire_sets where title='Quick Fire: Entertainment & Sport' limit 1
), inserted as (
 insert into public.arena_quickfire_questions(set_id,question_text,options,position,points)
 select chosen_set.id,q.question_text,q.options,q.position,10
 from q cross join chosen_set
 where not exists(select 1 from public.arena_quickfire_questions old
   where old.set_id=chosen_set.id and old.question_text=q.question_text)
 returning id,position
)
insert into private.arena_quickfire_answer_keys(question_id,correct_option_index,explanation)
select inserted.id,q.correct_index,q.explanation
from inserted join q using(position)
on conflict(question_id) do nothing;