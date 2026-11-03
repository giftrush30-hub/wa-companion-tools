with prompts(title,instructions,category) as (values
('The Data Finally Arrived','Caption this: You finally buy data, open your favourite app, and the network starts moving like it is on holiday. Keep it friendly, original and under 500 characters.','Everyday Naija'),
('When Salary Enters','Caption this: Salary alert arrives, and before you can even smile, five bills are already waiting at the door. Keep it funny, respectful and under 500 characters.','Money Matters'),
('The Family WhatsApp Group','Caption this: Someone drops a Good morning message with 14 flowers, 8 doves and a long prayer before 6am. Keep it playful, not insulting.','Family & Friends'),
('Lagos Traffic Chronicles','Caption this: You left home early, but the traffic has other plans. Give us your funniest clean caption.','City Life'),
('The Uninvited Visitor','Caption this: You just sat down to eat and someone says, Hope you do not mind if I join you? Make it relatable and kind.','Everyday Naija'),
('When NEPA Takes Light','Caption this: The power goes off exactly when your phone is at 2% and you are about to send an important message. Keep it original.','Everyday Naija'),
('The Group Project','Caption this: The group assignment is due tonight, but one group member has only contributed Good evening everyone. Keep it clean.','School & Work'),
('Small Business Hustle','Caption this: A customer asks for your best price, then says they saw the same thing cheaper somewhere else. Keep it funny and respectful.','Business & Money'),
('The Weekend Plan','Caption this: You planned a big weekend outing, but your bed and blanket have started negotiating with you. Make us laugh.','Lifestyle'),
('The Phone Battery Drama','Caption this: Your phone drops from 20% to 1% just as you open the camera to capture something important. Write a relatable caption.','Tech & Digital Life')
)
insert into public.arena_meme_challenges(title,instructions,category,status,starts_at,ends_at)
select p.title,p.instructions,p.category,'published',now(),now()+interval '14 days'
from prompts p
where not exists(select 1 from public.arena_meme_challenges c where c.title=p.title);
