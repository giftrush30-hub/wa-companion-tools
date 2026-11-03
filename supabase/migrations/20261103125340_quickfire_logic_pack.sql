insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: Words, Logic & Everyday Skills','Language, reasoning, communication and practical everyday knowledge.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Words, Logic & Everyday Skills');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'Which word is closest in meaning to brief?','["Short","Heavy","Late","Loud"]'::jsonb,0,'Brief means short in duration or length.'),
(2,'What is the opposite of generous?','["Kind","Stingy","Helpful","Patient"]'::jsonb,1,'Stingy means unwilling to give or share.'),
(3,'Which sentence is correctly punctuated?','["Lets eat grandma.","Let''s eat, Grandma.","Lets eat, Grandma","Let''s eat Grandma"]'::jsonb,1,'The comma addresses Grandma; the apostrophe means let us.'),
(4,'If all roses are flowers, which must be true?','["All flowers are roses","Every rose is a flower","No roses are flowers","All plants are roses"]'::jsonb,1,'This restates the given condition.'),
(5,'What comes next: 2, 4, 8, 16?','["18","24","32","36"]'::jsonb,2,'Each number doubles.'),
(6,'If today is Tuesday, what day is it in three days?','["Wednesday","Thursday","Friday","Saturday"]'::jsonb,2,'Three days after Tuesday is Friday.'),
(7,'If you do not understand an instruction, what should you do?','["Pretend","Ask a clear question","Blame someone","Ignore it"]'::jsonb,1,'Clarifying early prevents mistakes.'),
(8,'Which pair are synonyms?','["Begin and start","Hot and cold","Open and shut","Early and late"]'::jsonb,0,'Begin and start have similar meanings.'),
(9,'A shop has 12 oranges and sells 5. How many remain?','["5","6","7","8"]'::jsonb,2,'12 minus 5 equals 7.'),
(10,'What is active listening?','["Interrupting","Paying attention and asking relevant questions","Looking away","Changing topic"]'::jsonb,1,'Active listening includes attention and clarification.'),
(11,'What comes next: A, C, E, G?','["H","I","J","K"]'::jsonb,1,'The pattern skips one letter each time.'),
(12,'Where should you check an official government announcement?','["Anonymous forward","Government website or verified channel","Random comment","Edited screenshot"]'::jsonb,1,'Official channels are primary sources.'),
(13,'What does compare mean?','["Find similarities and differences","Copy exactly","Delete information","Guess"]'::jsonb,0,'To compare is to examine similarities and differences.'),
(14,'If 3 notebooks cost ₦900 equally, what is one notebook?','["₦200","₦250","₦300","₦350"]'::jsonb,2,'₦900 divided by 3 is ₦300.'),
(15,'Which habit best supports learning?','["Practice regularly and review mistakes","Study once","Avoid feedback","Skip difficult steps"]'::jsonb,0,'Practice and reflection help build skills.')
), s as (select id from public.arena_quickfire_sets where title='Quick Fire: Words, Logic & Everyday Skills' limit 1),
i as (
 insert into public.arena_quickfire_questions(set_id,question_text,options,position,points,difficulty)
 select s.id,q.question_text,q.options,q.position,10,
 case when q.position<=5 then 'easy' when q.position<=10 then 'standard' else 'expert' end
 from q cross join s
 where not exists(select 1 from public.arena_quickfire_questions old where old.set_id=s.id and old.question_text=q.question_text)
 returning id,position
)
insert into private.arena_quickfire_answer_keys(question_id,correct_option_index,explanation)
select i.id,q.correct_index,q.explanation from i join q using(position)
on conflict(question_id) do nothing;
