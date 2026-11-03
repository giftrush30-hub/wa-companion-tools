-- WA Discover: lean WhatsApp-first business directory.
-- Apply manually in the Supabase SQL Editor after creating your project.
-- No product images, media uploads, paid promotions, or public seller accounts in v1.
-- Admin access is granted only through trusted Supabase app_metadata, never user-editable user_metadata.

create extension if not exists pgcrypto;

create table if not exists public.discover_categories (
  id text primary key,
  name text not null unique,
  sort_order integer not null default 0,
  active boolean not null default true
);

insert into public.discover_categories (id, name, sort_order) values
  ('fashion', 'Fashion', 1),
  ('food', 'Food & Groceries', 2),
  ('phones', 'Phones & Accessories', 3),
  ('beauty', 'Beauty', 4),
  ('electronics', 'Electronics', 5),
  ('home', 'Home & Living', 6),
  ('farming', 'Farming', 7),
  ('services', 'Services', 8),
  ('other', 'Other', 9)
on conflict (id) do nothing;

create table if not exists public.discover_businesses (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 2 and 100),
  category_id text not null references public.discover_categories(id),
  description text not null default '' check (char_length(description) <= 1000),
  whatsapp_number text not null check (whatsapp_number ~ '^[1-9][0-9]{9,14}$'),
  catalogue_url text check (
    catalogue_url is null or
    (catalogue_url ~ '^https://[^[:space:]]+$' and char_length(catalogue_url) <= 500)
  ),
  state text not null check (char_length(trim(state)) between 2 and 60),
  city text not null check (char_length(trim(city)) between 2 and 80),
  approval_status text not null default 'pending'
    check (approval_status in ('pending', 'approved', 'rejected', 'suspended')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists discover_businesses_public_search_idx
  on public.discover_businesses (state, city, category_id, name)
  where approval_status = 'approved';

create table if not exists public.discover_reports (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.discover_businesses(id) on delete cascade,
  reason text not null check (char_length(trim(reason)) between 5 and 500),
  status text not null default 'open'
    check (status in ('open', 'reviewing', 'resolved', 'dismissed')),
  created_at timestamptz not null default now()
);

-- Paid promotion catalogue and payment-order records are prepared here.
-- Checkout and activation must wait for trusted server-side Paystack verification.
create table if not exists public.discover_promotion_packages (
  code text primary key,
  name text not null,
  price_naira integer not null check (price_naira >= 0),
  placement_description text not null,
  active boolean not null default true,
  sort_order integer not null default 0
);

insert into public.discover_promotion_packages (code, name, price_naira, placement_description, sort_order) values
  ('free', 'Free Basic Listing', 0, 'Standard approved directory listing', 0),
  ('local_50', 'Local Boost', 50, 'Sponsored placement for local discovery', 1),
  ('wider_100', 'Wider Discovery', 100, 'Sponsored placement across wider WA Discover discovery', 2),
  ('featured_300', 'Featured Listing', 300, 'More prominent Sponsored placement in discovery', 3),
  ('spotlight_1000', 'Business Spotlight', 1000, 'Highest-visibility Sponsored placement in discovery', 4)
on conflict (code) do update set name = excluded.name,
  price_naira = excluded.price_naira,
  placement_description = excluded.placement_description,
  sort_order = excluded.sort_order;

create table if not exists public.discover_promotion_orders (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.discover_businesses(id) on delete cascade,
  package_code text not null references public.discover_promotion_packages(code),
  amount_naira integer not null check (amount_naira > 0),
  payment_provider text not null default 'paystack' check (payment_provider = 'paystack'),
  payment_reference text unique,
  payment_status text not null default 'pending'
    check (payment_status in ('pending', 'paid', 'failed', 'refunded')),
  provider_transaction_id text,
  verified_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists discover_promotion_orders_business_idx
  on public.discover_promotion_orders (business_id, created_at desc);

create table if not exists public.discover_events (
  id bigint generated always as identity primary key,
  business_id uuid not null references public.discover_businesses(id) on delete cascade,
  event_type text not null check (event_type in ('profile_open', 'catalogue_tap', 'whatsapp_tap')),
  created_at timestamptz not null default now()
);

create or replace function public.is_wa_discover_admin()
returns boolean
language sql
stable
as $$
  select coalesce(auth.jwt() -> 'app_metadata' ->> 'role', '') = 'discover_admin';
$$;

-- Sellers may edit their business details, but cannot change owner or approval status.
create or replace function public.guard_discover_business()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if auth.role() <> 'service_role' and not public.is_wa_discover_admin() then
    if tg_op = 'INSERT' then
      if new.owner_id <> auth.uid() or new.approval_status <> 'pending' then
        raise exception 'Businesses must be submitted by their owner for review';
      end if;
    else
      if new.owner_id is distinct from old.owner_id
         or new.approval_status is distinct from old.approval_status then
        raise exception 'Only WA Discover administrators can change ownership or approval status';
      end if;
    end if;
  end if;
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists discover_business_guard on public.discover_businesses;
create trigger discover_business_guard
  before insert or update on public.discover_businesses
  for each row execute function public.guard_discover_business();

alter table public.discover_categories enable row level security;
alter table public.discover_businesses enable row level security;
alter table public.discover_reports enable row level security;
alter table public.discover_events enable row level security;

drop policy if exists "Anyone reads active discover categories" on public.discover_categories;
create policy "Anyone reads active discover categories"
  on public.discover_categories for select to anon, authenticated
  using (active = true);

drop policy if exists "Public reads approved discover businesses" on public.discover_businesses;
create policy "Public reads approved discover businesses"
  on public.discover_businesses for select to anon, authenticated
  using (approval_status = 'approved' or owner_id = auth.uid() or public.is_wa_discover_admin());

drop policy if exists "Sellers submit pending discover businesses" on public.discover_businesses;
create policy "Sellers submit pending discover businesses"
  on public.discover_businesses for insert to authenticated
  with check (owner_id = auth.uid() and approval_status = 'pending');

drop policy if exists "Sellers update own discover businesses" on public.discover_businesses;
create policy "Sellers update own discover businesses"
  on public.discover_businesses for update to authenticated
  using (owner_id = auth.uid() or public.is_wa_discover_admin())
  with check (owner_id = auth.uid() or public.is_wa_discover_admin());

drop policy if exists "Anyone may report a discover business" on public.discover_reports;
create policy "Anyone may report a discover business"
  on public.discover_reports for insert to anon, authenticated
  with check (char_length(trim(reason)) between 5 and 500);

drop policy if exists "Only admins read discover reports" on public.discover_reports;
create policy "Only admins read discover reports"
  on public.discover_reports for select to authenticated
  using (public.is_wa_discover_admin());

drop policy if exists "Only admins moderate discover reports" on public.discover_reports;
create policy "Only admins moderate discover reports"
  on public.discover_reports for update to authenticated
  using (public.is_wa_discover_admin())
  with check (public.is_wa_discover_admin());

drop policy if exists "Public records discover engagement events" on public.discover_events;
create policy "Public records discover engagement events"
  on public.discover_events for insert to anon, authenticated
  with check (exists (
    select 1 from public.discover_businesses b
    where b.id = business_id and b.approval_status = 'approved'
  ));

drop policy if exists "Business owners and admins read discover events" on public.discover_events;
create policy "Business owners and admins read discover events"
  on public.discover_events for select to authenticated
  using (
    public.is_wa_discover_admin()
    or exists (
      select 1 from public.discover_businesses b
      where b.id = business_id and b.owner_id = auth.uid()
    )
  );


alter table public.discover_promotion_packages enable row level security;
alter table public.discover_promotion_orders enable row level security;

drop policy if exists "Anyone reads active promotion packages" on public.discover_promotion_packages;
create policy "Anyone reads active promotion packages"
  on public.discover_promotion_packages for select to anon, authenticated
  using (active = true);

drop policy if exists "Owners read own promotion orders" on public.discover_promotion_orders;
create policy "Owners read own promotion orders"
  on public.discover_promotion_orders for select to authenticated
  using (exists (
    select 1 from public.discover_businesses b
    where b.id = business_id and b.owner_id = auth.uid()
  ) or public.is_wa_discover_admin());

drop policy if exists "Owners create pending promotion orders" on public.discover_promotion_orders;
create policy "Owners create pending promotion orders"
  on public.discover_promotion_orders for insert to authenticated
  with check (
    payment_status = 'pending' and payment_provider = 'paystack'
    and exists (
      select 1 from public.discover_businesses b
      where b.id = business_id and b.owner_id = auth.uid()
        and b.approval_status = 'approved'
    )
    and exists (
      select 1 from public.discover_promotion_packages p
      where p.code = package_code and p.active = true
        and p.price_naira = amount_naira and p.price_naira > 0
    )
  );

drop policy if exists "Admins update promotion orders" on public.discover_promotion_orders;
create policy "Admins update promotion orders"
  on public.discover_promotion_orders for update to authenticated
  using (public.is_wa_discover_admin())
  with check (public.is_wa_discover_admin());

-- IMPORTANT:
-- 1. This SQL has NOT been run against a live Supabase project.
-- 2. Grant the discover_admin role only via trusted server-side admin tooling.
-- 3. Never put a Supabase service-role key in Android source code.
-- 4. Promotion prices are prepared, but checkout is inactive until server-side Paystack verification exists.
-- 5. Public events are coarse click counts, not WhatsApp Status views or sales.
