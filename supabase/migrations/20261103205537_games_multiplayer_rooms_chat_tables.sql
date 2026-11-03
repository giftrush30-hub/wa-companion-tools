-- Multiplayer room and chat foundation for Games Arena.
-- Match-state validation and voice-token issuance are separate server milestones.

create table public.game_rooms (
  id uuid primary key default gen_random_uuid(),
  room_code text not null unique check (room_code ~ '^[A-F0-9]{8}$'),
  game_type text not null check (game_type in ('big_quiz','ludo','whot')),
  visibility text not null default 'private' check (visibility in ('private','public')),
  status text not null default 'waiting' check (status in ('waiting','in_progress','completed','cancelled')),
  host_id uuid not null references auth.users(id) on delete cascade,
  max_players smallint not null check (max_players between 2 and 8),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '2 hours')
);

create table public.game_room_members (
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  display_name text not null check (char_length(btrim(display_name)) between 2 and 40),
  role text not null default 'player' check (role in ('host','player')),
  is_ready boolean not null default false,
  joined_at timestamptz not null default now(),
  left_at timestamptz,
  primary key (room_id,user_id)
);

create table public.game_chat_messages (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(btrim(body)) between 1 and 300),
  created_at timestamptz not null default now()
);

create index game_rooms_public_waiting_idx on public.game_rooms(game_type,created_at desc)
  where visibility='public' and status='waiting';
create index game_rooms_host_id_idx on public.game_rooms(host_id);
create index game_room_members_user_active_idx on public.game_room_members(user_id,joined_at desc)
  where left_at is null;
create index game_chat_messages_room_created_idx on public.game_chat_messages(room_id,created_at desc);
create index game_chat_messages_sender_id_idx on public.game_chat_messages(sender_id);

alter table public.game_rooms enable row level security;
alter table public.game_room_members enable row level security;
alter table public.game_chat_messages enable row level security;

grant select on public.game_rooms to authenticated;
grant select on public.game_room_members to authenticated;
grant select,insert on public.game_chat_messages to authenticated;

create or replace function private.is_game_room_member(p_room_id uuid)
returns boolean language sql stable security definer set search_path=''
as $fn$
  select exists (
    select 1 from public.game_room_members m
    where m.room_id=p_room_id and m.user_id=auth.uid() and m.left_at is null
  );
$fn$;
grant usage on schema private to authenticated;
revoke all on function private.is_game_room_member(uuid) from public,anon;
grant execute on function private.is_game_room_member(uuid) to authenticated;

create policy "members and public lobby can read game rooms"
on public.game_rooms for select to authenticated
using ((visibility='public' and status='waiting' and expires_at>now()) or private.is_game_room_member(id));

create policy "room members can read co-player list"
on public.game_room_members for select to authenticated
using (private.is_game_room_member(room_id));

create policy "active room members can read chat"
on public.game_chat_messages for select to authenticated
using (private.is_game_room_member(room_id));

create policy "active room members can send chat"
on public.game_chat_messages for insert to authenticated
with check (
  sender_id=(select auth.uid())
  and char_length(btrim(body)) between 1 and 300
  and private.is_game_room_member(room_id)
  and exists(select 1 from public.game_rooms r where r.id=room_id
    and r.status in ('waiting','in_progress') and r.expires_at>now())
);

create or replace function public.create_game_room(p_game_type text,p_visibility text,p_display_name text,p_max_players integer)
returns jsonb language plpgsql security definer set search_path=''
as $fn$
declare
  v_user uuid := auth.uid();
  v_room public.game_rooms%rowtype;
  v_code text;
begin
  if v_user is null then raise exception 'Sign in to create a game room.'; end if;
  if p_game_type not in ('big_quiz','ludo','whot') then raise exception 'Choose a supported game.'; end if;
  if p_visibility not in ('private','public') then raise exception 'Choose private or public room.'; end if;
  if char_length(btrim(coalesce(p_display_name,''))) not between 2 and 40 then raise exception 'Display name must be 2-40 characters.'; end if;
  if p_max_players < 2 or p_max_players > (case when p_game_type='big_quiz' then 8 else 4 end) then
    raise exception 'Invalid player limit for this game.';
  end if;
  loop
    v_code := upper(substr(replace(gen_random_uuid()::text,'-',''),1,8));
    exit when not exists(select 1 from public.game_rooms where room_code=v_code);
  end loop;
  insert into public.game_rooms(room_code,game_type,visibility,host_id,max_players)
    values(v_code,p_game_type,p_visibility,v_user,p_max_players) returning * into v_room;
  insert into public.game_room_members(room_id,user_id,display_name,role)
    values(v_room.id,v_user,btrim(p_display_name),'host');
  return jsonb_build_object('room_id',v_room.id,'room_code',v_room.room_code,'game_type',v_room.game_type,
    'visibility',v_room.visibility,'status',v_room.status,'max_players',v_room.max_players);
end;
$fn$;

create or replace function public.join_game_room(p_room_code text,p_display_name text)
returns jsonb language plpgsql security definer set search_path=''
as $fn$
declare
  v_user uuid := auth.uid();
  v_room public.game_rooms%rowtype;
  v_existing boolean;
  v_count integer;
begin
  if v_user is null then raise exception 'Sign in to join a game room.'; end if;
  if char_length(btrim(coalesce(p_display_name,''))) not between 2 and 40 then raise exception 'Display name must be 2-40 characters.'; end if;
  select * into v_room from public.game_rooms
    where room_code=upper(btrim(p_room_code)) and status='waiting' and expires_at>now() for update;
  if not found then raise exception 'Room not found, closed or expired.'; end if;
  select exists(select 1 from public.game_room_members where room_id=v_room.id and user_id=v_user and left_at is null) into v_existing;
  select count(*) into v_count from public.game_room_members where room_id=v_room.id and left_at is null;
  if not v_existing and v_count>=v_room.max_players then raise exception 'This room is full.'; end if;
  insert into public.game_room_members(room_id,user_id,display_name,role,is_ready,left_at)
    values(v_room.id,v_user,btrim(p_display_name),'player',false,null)
  on conflict(room_id,user_id) do update
    set display_name=excluded.display_name,is_ready=false,left_at=null,joined_at=now();
  return jsonb_build_object('room_id',v_room.id,'room_code',v_room.room_code,'game_type',v_room.game_type,
    'visibility',v_room.visibility,'status',v_room.status,'max_players',v_room.max_players);
end;
$fn$;

create or replace function public.set_game_room_ready(p_room_id uuid,p_ready boolean)
returns boolean language plpgsql security definer set search_path=''
as $fn$
begin
  if auth.uid() is null then raise exception 'Sign in first.'; end if;
  update public.game_room_members set is_ready=coalesce(p_ready,false)
    where room_id=p_room_id and user_id=auth.uid() and left_at is null;
  if not found then raise exception 'You are not an active member of this room.'; end if;
  return true;
end;
$fn$;

create or replace function public.leave_game_room(p_room_id uuid)
returns boolean language plpgsql security definer set search_path=''
as $fn$
declare
  v_host uuid;
  v_next uuid;
begin
  if auth.uid() is null then raise exception 'Sign in first.'; end if;
  select host_id into v_host from public.game_rooms where id=p_room_id for update;
  if not found then return false; end if;
  update public.game_room_members set left_at=now(),is_ready=false
    where room_id=p_room_id and user_id=auth.uid() and left_at is null;
  if v_host=auth.uid() then
    select user_id into v_next from public.game_room_members
      where room_id=p_room_id and left_at is null order by joined_at asc limit 1;
    if v_next is null then
      update public.game_rooms set status='cancelled' where id=p_room_id and status='waiting';
    else
      update public.game_rooms set host_id=v_next where id=p_room_id and status='waiting';
      update public.game_room_members set role='host' where room_id=p_room_id and user_id=v_next;
    end if;
  end if;
  return true;
end;
$fn$;

revoke all on function public.create_game_room(text,text,text,integer) from public,anon;
revoke all on function public.join_game_room(text,text) from public,anon;
revoke all on function public.set_game_room_ready(uuid,boolean) from public,anon;
revoke all on function public.leave_game_room(uuid) from public,anon;
grant execute on function public.create_game_room(text,text,text,integer) to authenticated;
grant execute on function public.join_game_room(text,text) to authenticated;
grant execute on function public.set_game_room_ready(uuid,boolean) to authenticated;
grant execute on function public.leave_game_room(uuid) to authenticated;

create policy "room members can receive private game broadcasts"
on realtime.messages for select to authenticated
using (
  realtime.messages.extension in ('broadcast','presence')
  and case
    when split_part(realtime.topic(),':',3) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    then private.is_game_room_member(split_part(realtime.topic(),':',3)::uuid)
    else false
  end
);

create policy "room members can send private game broadcasts"
on realtime.messages for insert to authenticated
with check (
  realtime.messages.extension in ('broadcast','presence')
  and case
    when split_part(realtime.topic(),':',3) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    then private.is_game_room_member(split_part(realtime.topic(),':',3)::uuid)
    else false
  end
);
