-- WA Discover initial schema for Supabase PostgreSQL.
-- Run this in the Supabase SQL Editor only after creating the project.
-- Public users can read approved listings; sellers can manage only their own records.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  phone_verified boolean not null default false,
  is_admin boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.categories (
  id text primary key,
  name text not null unique,
  sort_order integer not null default 0,
  active boolean not null default true
);

insert into public.categories (id, name, sort_order) values
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

create table if not exists public.businesses (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  name text not null check (char_length(name) between 2 and 100),
  category_id text not null references public.categories(id),
  description text not null default '' check (char_length(description) <= 1500),
  whatsapp_number text not null,
  state text not null,
  city text not null,
  address text,
  logo_path text,
  approval_status text not null default 'pending'
    check (approval_status in ('pending', 'approved', 'rejected', 'suspended')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.listings (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  title text not null check (char_length(title) between 2 and 120),
  description text not null default '' check (char_length(description) <= 2000),
  listing_type text not null default 'product'
    check (listing_type in ('product', 'service')),
  price_ngn numeric(14,2) check (price_ngn is null or price_ngn >= 0),
  price_on_request boolean not null default false,
  image_path text,
  active boolean not null default true,
  approval_status text not null default 'pending'
    check (approval_status in ('pending', 'approved', 'rejected', 'suspended')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.promotions (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  package_code text not null check (package_code in ('local_50', 'wide_100', 'featured_300', 'spotlight_1000')),
  amount_ngn integer not null check (amount_ngn > 0),
  starts_at timestamptz,
  ends_at timestamptz,
  payment_reference text unique,
  payment_status text not null default 'pending'
    check (payment_status in ('pending', 'paid', 'failed', 'refunded')),
  approval_status text not null default 'pending'
    check (approval_status in ('pending', 'approved', 'rejected', 'paused')),
  created_at timestamptz not null default now()
);

create table if not exists public.listing_events (
  id bigint generated always as identity primary key,
  listing_id uuid not null references public.listings(id) on delete cascade,
  event_type text not null check (event_type in ('impression', 'open', 'save', 'whatsapp_tap')),
  visitor_key text,
  actor_id uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now()
);

create table if not exists public.listing_reports (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid references public.listings(id) on delete set null,
  business_id uuid references public.businesses(id) on delete set null,
  reporter_id uuid references public.profiles(id) on delete set null,
  reason text not null check (char_length(reason) between 5 and 1000),
  status text not null default 'open' check (status in ('open', 'reviewing', 'resolved', 'dismissed')),
  created_at timestamptz not null default now(),
  check (listing_id is not null or business_id is not null)
);

create index if not exists businesses_public_location_idx
  on public.businesses (state, city, category_id) where approval_status = 'approved';
create index if not exists listings_public_feed_idx
  on public.listings (created_at desc) where active = true and approval_status = 'approved';
create index if not exists listings_business_idx on public.listings (business_id);
create index if not exists listing_events_listing_date_idx on public.listing_events (listing_id, created_at desc);

create or replace function public.is_discover_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and is_admin = true
  );
$$;

alter table public.profiles enable row level security;
alter table public.categories enable row level security;
alter table public.businesses enable row level security;
alter table public.listings enable row level security;
alter table public.promotions enable row level security;
alter table public.listing_events enable row level security;
alter table public.listing_reports enable row level security;

drop policy if exists "Profiles readable by owner or admin" on public.profiles;
create policy "Profiles readable by owner or admin" on public.profiles
  for select to authenticated using (id = auth.uid() or public.is_discover_admin());
drop policy if exists "Users create own profile" on public.profiles;
create policy "Users create own profile" on public.profiles
  for insert to authenticated with check (id = auth.uid() and is_admin = false);
drop policy if exists "Users update own profile" on public.profiles;
create policy "Users update own profile" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid() and is_admin = false);

drop policy if exists "Anyone reads active categories" on public.categories;
create policy "Anyone reads active categories" on public.categories
  for select to anon, authenticated using (active = true);

drop policy if exists "Public reads approved businesses" on public.businesses;
create policy "Public reads approved businesses" on public.businesses
  for select to anon, authenticated using (approval_status = 'approved' or owner_id = auth.uid() or public.is_discover_admin());
drop policy if exists "Owners create businesses" on public.businesses;
create policy "Owners create businesses" on public.businesses
  for insert to authenticated with check (owner_id = auth.uid() and approval_status = 'pending');
drop policy if exists "Owners update own businesses" on public.businesses;
create policy "Owners update own businesses" on public.businesses
  for update to authenticated using (owner_id = auth.uid() or public.is_discover_admin())
  with check (owner_id = auth.uid() or public.is_discover_admin());

drop policy if exists "Public reads approved listings" on public.listings;
create policy "Public reads approved listings" on public.listings
  for select to anon, authenticated using (
    (approval_status = 'approved' and active = true)
    or owner_id = auth.uid()
    or public.is_discover_admin()
  );
drop policy if exists "Owners create pending listings" on public.listings;
create policy "Owners create pending listings" on public.listings
  for insert to authenticated with check (
    owner_id = auth.uid() and approval_status = 'pending'
    and exists (
      select 1 from public.businesses b
      where b.id = business_id and b.owner_id = auth.uid()
    )
  );
drop policy if exists "Owners update own listings" on public.listings;
create policy "Owners update own listings" on public.listings
  for update to authenticated using (owner_id = auth.uid() or public.is_discover_admin())
  with check (owner_id = auth.uid() or public.is_discover_admin());

drop policy if exists "Owners and admins read promotions" on public.promotions;
create policy "Owners and admins read promotions" on public.promotions
  for select to authenticated using (owner_id = auth.uid() or public.is_discover_admin());

drop policy if exists "Public may record listing events" on public.listing_events;
create policy "Public may record listing events" on public.listing_events
  for insert to anon, authenticated with check (
    event_type in ('impression', 'open', 'save', 'whatsapp_tap')
    and exists (
      select 1 from public.listings l
      where l.id = listing_id and l.approval_status = 'approved' and l.active = true
    )
  );
drop policy if exists "Owners and admins read listing events" on public.listing_events;
create policy "Owners and admins read listing events" on public.listing_events
  for select to authenticated using (
    public.is_discover_admin()
    or exists (
      select 1 from public.listings l
      where l.id = listing_id and l.owner_id = auth.uid()
    )
  );

drop policy if exists "Anyone can submit listing reports" on public.listing_reports;
create policy "Anyone can submit listing reports" on public.listing_reports
  for insert to anon, authenticated with check (char_length(reason) between 5 and 1000);
drop policy if exists "Admins review listing reports" on public.listing_reports;
create policy "Admins review listing reports" on public.listing_reports
  for select to authenticated using (public.is_discover_admin());
drop policy if exists "Admins update listing reports" on public.listing_reports;
create policy "Admins update listing reports" on public.listing_reports
  for update to authenticated using (public.is_discover_admin())
  with check (public.is_discover_admin());

-- IMPORTANT:
-- Never place the Supabase service-role key in the Android app.
-- Configure private storage buckets and storage.objects policies separately before
-- allowing sellers to upload logos or listing photos.
-- Do not enable promotion purchases until payment verification is handled server-side.
