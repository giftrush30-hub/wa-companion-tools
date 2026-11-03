alter table public.arena_meme_challenges
  add column if not exists template_credit text;

update public.arena_meme_challenges
set template_image_url='https://images.unsplash.com/photo-1536692192939-f1547f1cde39?auto=format&fit=crop&w=1200&q=80',
    template_credit='Photo by Alexander Andrews on Unsplash'
where title='The Phone Battery Drama';

update public.arena_meme_challenges
set template_image_url='https://images.unsplash.com/photo-1648023199223-25d3622bcb13?auto=format&fit=crop&w=1200&q=80',
    template_credit='Photo by Opeyemi Adisa on Unsplash'
where title='Lagos Traffic Chronicles';

update public.arena_meme_challenges
set template_image_url='https://images.unsplash.com/photo-1753351055246-a7efae066eef?auto=format&fit=crop&w=1200&q=80',
    template_credit='Photo by Vitaly Gariev on Unsplash'
where title='The Family WhatsApp Group';

update public.arena_meme_challenges
set template_image_url='https://images.unsplash.com/photo-1768483538267-fce52de424d5?auto=format&fit=crop&w=1200&q=80',
    template_credit='Photo by Gabriel Weyand on Unsplash'
where title='The Group Project';

insert into public.arena_meme_challenges(title,instructions,category,template_image_url,template_credit,status,starts_at,ends_at)
select 'The Jollof Debate',
       'The party jollof has arrived. One guest says it needs more pepper, another says it is perfect. Write the funniest friendly caption.',
       'Food & Naija',
       'https://images.unsplash.com/photo-1665556899022-9761f95769e5?auto=format&fit=crop&w=1200&q=80',
       'Photo by Keesha’s Kitchen on Unsplash',
       'published',now(),now()+interval '14 days'
where not exists(select 1 from public.arena_meme_challenges where title='The Jollof Debate');
