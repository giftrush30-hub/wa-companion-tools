insert into public.arena_meme_challenges(title,instructions,category,template_image_url,template_credit,status,starts_at,ends_at)
select 'The Jollof vs Fried Rice Debate','The food has arrived and everybody is suddenly a food critic. Write a funny, friendly caption that keeps the debate playful.','Food & Naija','https://images.unsplash.com/photo-1638436684761-7e59f8a9072f?auto=format&fit=crop&w=1200&q=80','Photo by Chibuzo Nwaneri on Unsplash','published',now(),now()+interval '30 days'
where not exists(select 1 from public.arena_meme_challenges where title='The Jollof vs Fried Rice Debate');

insert into public.arena_meme_challenges(title,instructions,category,template_image_url,template_credit,status,starts_at,ends_at)
select 'The Friends Group Picture','Everybody said they were ready for the picture, but one person blinked, one person looked away and one person is still fixing their hair. Caption the moment kindly.','Friends & Lifestyle','https://images.unsplash.com/photo-1755705153160-67b29c7718ee?auto=format&fit=crop&w=1200&q=80','Photo by Ufoma Ojo on Unsplash','published',now(),now()+interval '30 days'
where not exists(select 1 from public.arena_meme_challenges where title='The Friends Group Picture');

insert into public.arena_meme_challenges(title,instructions,category,template_image_url,template_credit,status,starts_at,ends_at)
select 'Deadline Is Tonight','The deadline is 11:59pm, the laptop is open, and the group chat has suddenly gone quiet. Write a relatable caption.','School & Work','https://images.unsplash.com/photo-1640622842223-e1e39f4bf627?auto=format&fit=crop&w=1200&q=80','Photo by Microsoft 365 on Unsplash','published',now(),now()+interval '30 days'
where not exists(select 1 from public.arena_meme_challenges where title='Deadline Is Tonight');
