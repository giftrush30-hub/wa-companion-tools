insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: Naija & Culture','Nigerian places, culture, food, language and everyday life.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Naija & Culture');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'Which Nigerian river is one of the country''s longest?','["Benue","Niger","Osun","Ogun"]'::jsonb,1,'The River Niger is one of West Africa''s major rivers and gives Nigeria its name.'),
(2,'What is the capital of Oyo State?','["Ibadan","Abeokuta","Osogbo","Ilorin"]'::jsonb,0,'Ibadan is the capital of Oyo State.'),
(3,'Which Nigerian festival is strongly associated with Argungu?','["Osun-Osogbo","Argungu Fishing Festival","Durbar","New Yam Festival"]'::jsonb,1,'The Argungu Fishing Festival is held in Kebbi State.'),
(4,'What is the main ingredient in traditional Nigerian moi moi?','["Cassava","Beans","Yam","Plantain"]'::jsonb,1,'Moi moi is commonly made from blended beans.'),
(5,'Which city is popularly called the Coal City?','["Enugu","Jos","Uyo","Akure"]'::jsonb,0,'Enugu is known as the Coal City because of its coal-mining history.'),
(6,'Which Nigerian state is home to Olumo Rock?','["Ekiti","Ogun","Kwara","Edo"]'::jsonb,1,'Olumo Rock is a major landmark in Abeokuta, Ogun State.'),
(7,'Which language is widely spoken by the Hausa people?','["Igbo","Tiv","Hausa","Efik"]'::jsonb,2,'Hausa is widely spoken across northern Nigeria and beyond.'),
(8,'Suya is traditionally cooked over what?','["Deep fryer","Open charcoal grill","Steamer","Microwave"]'::jsonb,1,'Suya is commonly grilled over glowing charcoal.'),
(9,'Who wrote the novel ''Half of a Yellow Sun''?','["Chinua Achebe","Buchi Emecheta","Chimamanda Ngozi Adichie","Ben Okri"]'::jsonb,2,'Chimamanda Ngozi Adichie wrote the novel.'),
(10,'How many geopolitical zones does Nigeria have?','["4","5","6","7"]'::jsonb,2,'Nigeria is commonly grouped into six geopolitical zones.'),
(11,'Which food is commonly made from fermented cassava?','["Akara","Garri","Tuwo shinkafa","Masa"]'::jsonb,1,'Garri is made from processed, fermented cassava.'),
(12,'Which Nigerian city hosts famous Durbar celebrations?','["Kano","Warri","Calabar","Aba"]'::jsonb,0,'Kano is especially known for its colourful Durbar celebrations.'),
(13,'What is the official currency of Nigeria?','["Cedi","Naira","Dalasi","Shilling"]'::jsonb,1,'The naira is the official currency of Nigeria.'),
(14,'Which ocean borders Nigeria''s southern coastline?','["Indian Ocean","Pacific Ocean","Atlantic Ocean","Arctic Ocean"]'::jsonb,2,'Nigeria has a coastline along the Gulf of Guinea, part of the Atlantic Ocean.'),
(15,'Which city is Nigeria''s federal capital?','["Lagos","Abuja","Kano","Enugu"]'::jsonb,1,'Abuja became Nigeria''s federal capital in 1991.')
), chosen_set as (
 select id from public.arena_quickfire_sets where title='Quick Fire: Naija & Culture' limit 1
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
select 'Quick Fire: Tech & Digital Life','Smartphones, internet, computing and digital common sense.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Tech & Digital Life');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'What does URL stand for?','["Uniform Resource Locator","Universal Reading Link","User Route Login","Unified Record List"]'::jsonb,0,'URL means Uniform Resource Locator; it is a web address.'),
(2,'Which component is often called a computer''s brain?','["Monitor","CPU","Keyboard","Speaker"]'::jsonb,1,'The CPU processes instructions and calculations.'),
(3,'What does Wi-Fi allow devices to do?','["Print without ink","Connect to a wireless network","Charge faster","Increase screen size"]'::jsonb,1,'Wi-Fi connects devices to a network wirelessly.'),
(4,'Which file format commonly supports transparent web images?','["PNG","MP3","TXT","WAV"]'::jsonb,0,'PNG supports transparency and is widely used for graphics.'),
(5,'What is phishing?','["A phone battery issue","A trick to steal sensitive information","A screen protector","A faster internet plan"]'::jsonb,1,'Phishing uses deceptive messages or sites to steal information.'),
(6,'What does 2FA add to an account?','["A second verification step","Two usernames","Double storage","A second screen"]'::jsonb,0,'Two-factor authentication adds another proof of identity.'),
(7,'Which is the strongest password practice?','["Reuse one easy password","Use your birthday","Use long unique passwords","Share passwords with friends"]'::jsonb,2,'Long, unique passwords reduce the damage from reused credentials.'),
(8,'What does an app update often provide?','["Only a new wallpaper","Bug fixes and improvements","Free mobile data","A new SIM card"]'::jsonb,1,'Updates commonly fix bugs, improve security and add features.'),
(9,'Which device stores data when power is off?','["RAM","SSD","CPU cache only","Fan"]'::jsonb,1,'An SSD is non-volatile storage.'),
(10,'What does airplane mode generally do?','["Turns off all apps permanently","Disables cellular radios","Deletes photos","Boosts mobile signal"]'::jsonb,1,'Airplane mode disables cellular transmission.'),
(11,'Which symbol is commonly used in an email address?','["#","@","&","%"]'::jsonb,1,'The @ symbol separates the user name from the domain.'),
(12,'What is a QR code mainly used for?','["Scanning a code to open information","Cooling a phone","Increasing battery life","Blocking spam calls"]'::jsonb,0,'QR codes encode data that a camera can scan.'),
(13,'What does HTTPS indicate?','["A secure encrypted web connection","A free website","A website without images","A phone-only website"]'::jsonb,0,'HTTPS uses TLS to encrypt the connection between browser and site.'),
(14,'How can you best protect important phone photos?','["Keep only one copy","Back them up elsewhere","Delete the gallery app","Turn off the screen"]'::jsonb,1,'A separate backup protects photos if the phone is lost or damaged.'),
(15,'What does GPS help a phone determine?','["Location","Screen brightness","Battery temperature","Speaker volume"]'::jsonb,0,'GPS uses satellite signals to estimate location.')
), chosen_set as (
 select id from public.arena_quickfire_sets where title='Quick Fire: Tech & Digital Life' limit 1
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