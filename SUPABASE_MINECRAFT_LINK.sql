-- Crystal Ville website <-> Minecraft account linking
-- Already applied to the connected Crystal Ville Supabase project.
create or replace function public.complete_minecraft_link(p_code text, p_username text, p_uuid text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid;
  v_username text := trim(p_username);
begin
  if p_code is null or p_code !~ '^[0-9]{6}$' then
    return jsonb_build_object('success', false, 'error', 'Invalid code');
  end if;
  if v_username is null or v_username !~ '^[A-Za-z0-9_]{3,16}$' then
    return jsonb_build_object('success', false, 'error', 'Invalid Minecraft username');
  end if;
  if p_uuid is null or p_uuid !~ '^[0-9a-fA-F-]{36}$' then
    return jsonb_build_object('success', false, 'error', 'Invalid Minecraft UUID');
  end if;
  select pa.user_id into v_user_id
  from public.player_accounts pa
  where pa.verification_code = p_code and pa.verified = false
  limit 1;
  if v_user_id is null then
    return jsonb_build_object('success', false, 'error', 'Invalid or already-used code');
  end if;
  if exists (select 1 from public.player_accounts pa
             where lower(pa.minecraft_username)=lower(v_username) and pa.user_id <> v_user_id) then
    return jsonb_build_object('success', false, 'error', 'Minecraft username is already linked');
  end if;
  update public.player_accounts
     set minecraft_username=v_username, verified=true, updated_at=now()
   where user_id=v_user_id;
  insert into public.player_profiles (username, display_name, user_id)
  values (v_username, v_username, v_user_id)
  on conflict (username) do update
    set user_id=excluded.user_id, updated_at=now();
  return jsonb_build_object('success', true, 'user_id', v_user_id,
                            'minecraft_username', v_username, 'minecraft_uuid', p_uuid);
end;
$$;
revoke all on function public.complete_minecraft_link(text,text,text) from public;
grant execute on function public.complete_minecraft_link(text,text,text) to anon, authenticated;
