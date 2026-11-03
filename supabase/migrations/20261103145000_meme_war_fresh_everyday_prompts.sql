-- Ten more clean, relatable Meme War prompts for fresh caption rounds.
with prompts(title,instructions,category) as (values
('The “I Am Outside” Message','Caption this: Someone texts “I am outside” but you look out and there is absolutely nobody there. Keep it playful and relatable.','Everyday Naija'),
('The Unexpected Alert','Caption this: A bank alert enters and your face lights up before you remember the money already has assignments. Keep it clean and funny.','Money Matters'),
('The One Percent Network','Caption this: Your network shows full bars, but a simple message is taking longer than a family meeting.','Tech & Digital Life'),
('The Food Is Almost Ready','Caption this: Someone says food is ready, but the pot is still negotiating with the fire. Give us a clean caption.','Food & Lifestyle'),
('The Camera Is Open','Caption this: You open the front camera by mistake and meet yourself before you are emotionally prepared. Be kind, be funny.','Everyday Life'),
('The Monday Motivation','Caption this: It is Monday morning and your body is still requesting an extension on the weekend.','Work & School'),
('The Voice Note Marathon','Caption this: You receive a voice note so long that you start looking for chapter titles and a table of contents.','Family & Friends'),
('The Price Has Changed','Caption this: You return to buy an item and the seller says the price has changed since yesterday. Keep it respectful and humorous.','Business & Money'),
('The Group Chat Investigator','Caption this: Someone types “I have something to tell you” in the group chat, then disappears for three hours.','Friends & Social'),
('The Last Bus Seat','Caption this: You finally find a seat after a long day, then your stop appears almost immediately.','City Life')
)
insert into public.arena_meme_challenges(title,instructions,category,status,starts_at,ends_at)
select p.title,p.instructions,p.category,'published',now(),now()+interval '14 days'
from prompts p
where not exists(select 1 from public.arena_meme_challenges c where c.title=p.title);
