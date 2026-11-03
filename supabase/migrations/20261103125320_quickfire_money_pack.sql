insert into public.arena_quickfire_sets(title,description,status)
select 'Quick Fire: Money & Business','Everyday money skills, customer service, entrepreneurship and smart decisions.','published'
where not exists(select 1 from public.arena_quickfire_sets where title='Quick Fire: Money & Business');
with q(position,question_text,options,correct_index,explanation) as (values
(1,'What is a budget mainly used for?','["To plan income and spending","To borrow more money","To avoid saving","To hide expenses"]'::jsonb,0,'A budget helps plan income, expenses and savings.'),
(2,'What does profit mean in a business?','["Total sales only","Money left after costs","Number of customers","Money borrowed"]'::jsonb,1,'Profit is revenue minus business costs.'),
(3,'Which is a fixed business cost?','["Packaging per sale","Monthly shop rent","Delivery per order","Sales commission"]'::jsonb,1,'Rent is usually fixed for the agreed period.'),
(4,'Why keep sales records?','["Track performance and cash flow","Make products heavier","Avoid customers","Automatically raise prices"]'::jsonb,0,'Records show income, expenses and performance.'),
(5,'What is customer service?','["How a business helps customers","A bank loan","A product label","A tax form"]'::jsonb,0,'It is support before, during and after a purchase.'),
(6,'What is an emergency fund?','["Money for unexpected needs","Luxury spending money","An advert","A loan"]'::jsonb,0,'It helps cover unexpected expenses.'),
(7,'How should you confirm a bank transfer?','["Trust a screenshot","Check your bank account or official alert","Accept a promise","Share your PIN"]'::jsonb,1,'Verify payments in your own bank channel.'),
(8,'What is inventory?','["Goods held in stock","Staff birthdays","Ad slogans","Complaints"]'::jsonb,0,'Inventory is the stock held by a business.'),
(9,'A good first step before starting a business is to…','["Understand customer demand","Rent the biggest shop","Buy everything","Ignore competitors"]'::jsonb,0,'Research demand before investing heavily.'),
(10,'What is a receipt?','["Proof of a transaction","A slogan","A loan","A social post"]'::jsonb,0,'A receipt records a purchase or payment.'),
(11,'How can a business reduce waste?','["Track stock and expiry dates","Order without checking","Ignore damaged goods","Stop recording sales"]'::jsonb,0,'Stock tracking reduces over-ordering and spoilage.'),
(12,'What is revenue?','["Sales income before costs","Profit after all costs","Borrowed money","Personal savings"]'::jsonb,0,'Revenue is sales income before expenses.'),
(13,'Why keep banking PINs private?','["Protect accounts","Improve brightness","Speed up transfers","Increase airtime"]'::jsonb,0,'Private credentials reduce fraud risk.'),
(14,'What can customer reviews reveal?','["Customer experience","Owner''s age","Weather","Electricity cost"]'::jsonb,0,'Reviews show what customers value and what needs work.'),
(15,'How can you test a new product cheaply?','["Run a small pilot","Buy huge stock","Ignore feedback","Spend all savings on branding"]'::jsonb,0,'A small pilot tests demand before larger investment.')
), s as (select id from public.arena_quickfire_sets where title='Quick Fire: Money & Business' limit 1),
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
