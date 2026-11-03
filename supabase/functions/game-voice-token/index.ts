// Issues short-lived, room-scoped LiveKit audio tokens.
 // Required Supabase function secrets: LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET.
 // Never put the LiveKit API secret in the Android application.
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

async function signLiveKitToken(apiKey: string, apiSecret: string, identity: string, displayName: string, roomName: string) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(new TextEncoder().encode(JSON.stringify({ alg: "HS256", typ: "JWT" })));
  const claims = {
    iss: apiKey,
    sub: identity,
    name: displayName,
    nbf: now - 5,
    iat: now,
    exp: now + 15 * 60,
    video: {
      roomJoin: true,
      room: roomName,
      canPublish: true,
      canSubscribe: true,
      canPublishData: false,
    },
  };
  const payload = base64Url(new TextEncoder().encode(JSON.stringify(claims)));
  const unsigned = header + "." + payload;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(apiSecret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(unsigned)));
  return unsigned + "." + base64Url(signature);
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Method not allowed." }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseKey = Deno.env.get("SUPABASE_ANON_KEY");
  const livekitUrl = Deno.env.get("LIVEKIT_URL");
  const livekitKey = Deno.env.get("LIVEKIT_API_KEY");
  const livekitSecret = Deno.env.get("LIVEKIT_API_SECRET");
  if (!supabaseUrl || !supabaseKey || !livekitUrl || !livekitKey || !livekitSecret) {
    return json({ error: "Game voice is not configured yet." }, 503);
  }

  const authorization = request.headers.get("Authorization") ?? "";
  if (!authorization.startsWith("Bearer ")) return json({ error: "Sign in to join voice." }, 401);

  let input: { room_id?: string; display_name?: string };
  try {
    input = await request.json();
  } catch {
    return json({ error: "Invalid request body." }, 400);
  }
  const roomId = (input.room_id ?? "").trim();
  const displayName = (input.display_name ?? "").trim();
  if (!/^[0-9a-f-]{36}$/i.test(roomId)) return json({ error: "Invalid game room." }, 400);
  if (displayName.length < 2 || displayName.length > 40) return json({ error: "Player name must be 2–40 characters." }, 400);

  const headers = { apikey: supabaseKey, Authorization: authorization };
  const userResponse = await fetch(supabaseUrl + "/auth/v1/user", { headers });
  if (!userResponse.ok) return json({ error: "Your sign-in session is invalid. Sign in again." }, 401);
  const user = await userResponse.json();
  if (!user?.id) return json({ error: "Could not verify your player account." }, 401);

  const membershipUrl = new URL(supabaseUrl + "/rest/v1/game_room_members");
  membershipUrl.searchParams.set("select", "room_id");
  membershipUrl.searchParams.set("room_id", "eq." + roomId);
  membershipUrl.searchParams.set("user_id", "eq." + user.id);
  membershipUrl.searchParams.set("left_at", "is.null");
  const membershipResponse = await fetch(membershipUrl, { headers });
  if (!membershipResponse.ok) return json({ error: "Could not verify room membership." }, 403);
  const memberships = await membershipResponse.json();
  if (!Array.isArray(memberships) || memberships.length === 0) {
    return json({ error: "You are not an active member of this game room." }, 403);
  }

  const roomUrl = new URL(supabaseUrl + "/rest/v1/game_rooms");
  roomUrl.searchParams.set("select", "id,status");
  roomUrl.searchParams.set("id", "eq." + roomId);
  const roomResponse = await fetch(roomUrl, { headers });
  if (!roomResponse.ok) return json({ error: "Could not verify the game room." }, 403);
  const rooms = await roomResponse.json();
  if (!Array.isArray(rooms) || rooms.length === 0 || rooms[0].status !== "in_progress") {
    return json({ error: "Voice is available only while a match is in progress." }, 409);
  }

  const roomName = "wa-game-" + roomId;
  const token = await signLiveKitToken(livekitKey, livekitSecret, user.id, displayName, roomName);
  return json({ url: livekitUrl, token, room_name: roomName, expires_in: 900 });
});
