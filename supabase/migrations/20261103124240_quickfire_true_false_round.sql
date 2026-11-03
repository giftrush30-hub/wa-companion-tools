insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: True or False','A quick-fire round of true-or-false facts across Nigeria, technology, science and sport.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: True or False');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'Nigeria has 36 states.','["True","False"]'::jsonb,0,'Nigeria has 36 states and the Federal Capital Territory.'),
(2,'Lagos is the federal capital of Nigeria.','["True","False"]'::jsonb,1,'Abuja is Nigeria''s federal capital.'),
(3,'Water is commonly represented by the chemical formula H2O.','["True","False"]'::jsonb,0,'A water molecule contains two hydrogen atoms and one oxygen atom.'),
(4,'The Sun is a planet.','["True","False"]'::jsonb,1,'The Sun is a star.'),
(5,'Most adults have 206 bones.','["True","False"]'::jsonb,0,'A typical adult human skeleton has 206 bones.'),
(6,'A triangle has four sides.','["True","False"]'::jsonb,1,'A triangle has three sides.'),
(7,'HTTPS helps encrypt data sent between a browser and a website.','["True","False"]'::jsonb,0,'HTTPS uses TLS to protect data in transit.'),
(8,'Wi-Fi and the internet are exactly the same thing.','["True","False"]'::jsonb,1,'Wi-Fi is a wireless networking technology; it can connect a device to a local network or the internet.'),
(9,'A standard football team has eleven players on the pitch.','["True","False"]'::jsonb,0,'A standard team fields eleven players.'),
(10,'The Atlantic Ocean borders Nigeria''s southern coastline.','["True","False"]'::jsonb,0,'Nigeria''s coast faces the Gulf of Guinea, part of the Atlantic Ocean.'),
(11,'A leap year has 366 days.','["True","False"]'::jsonb,0,'A leap year includes an extra day in February.'),
(12,'The chemical symbol for gold is Ag.','["True","False"]'::jsonb,1,'Gold is Au; Ag is the symbol for silver.'),
(13,'Photosynthesis uses light energy.','["True","False"]'::jsonb,0,'Plants use light energy to make sugars from carbon dioxide and water.'),
(14,'Sound generally travels faster than light.','["True","False"]'::jsonb,1,'Light travels much faster than sound.'),
(15,'2FA stands for Two-Factor Authentication.','["True","False"]'::jsonb,0,'2FA adds a second verification factor to an account login.')
), chosen_set as (
 select id from public.arena_quickfire_sets where title='Quick Fire: True or False' limit 1
), inserted as (
 insert into public.arena_quickfire_questions(set_id,question_text,options,position,points)
 select chosen_set.id,q.question_text,q.options,q.position,10
 from q cross join chosen_set
 where not exists(select 1 from public.arena_quickfire_questions old where old.set_id=chosen_set.id and old.question_text=q.question_text)
 returning id,position
)
insert into private.arena_quickfire_answer_keys(question_id,correct_option_index,explanation)
select inserted.id,q.correct_index,q.explanation from inserted join q using(position)
on conflict(question_id) do nothing;
