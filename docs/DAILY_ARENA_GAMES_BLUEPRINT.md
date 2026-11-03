# WA Companion — Daily Arena & Games Blueprint

Status: Product blueprint and Phase 1 foundation. This document is the source of truth for the entertainment roadmap.

## Product vision
Add an engaging youth entertainment experience without replacing or damaging Discover, My Vault, My Diary or Referral Hub. Daily Arena provides lightweight daily participation; Games Arena provides skill-based and social games.

## 1. Daily Arena
A home-screen spotlight leads to:
- **Hot Seat:** daily opinion questions and polls, optional community-submitted questions, results, reporting and moderation.
- **Quick Fire:** short timed quizzes across general knowledge, Nigerian culture, entertainment, sports, technology and education.
- **Meme War:** original image/template caption contests, community voting, moderation and reporting.

Do not expose private information or encourage harassment, bullying, hateful content or humiliation. Anonymous submissions, if enabled, require stronger moderation and abuse controls.

## 2. Games Arena
Initial games:
- **Big Quiz:** daily competition plus unlimited practice. Original name, visual design, questions, sounds and content. Lifelines: 50:50, Ask the Audience and Phone a Friend. Phone a Friend opens the normal dialler only after the player deliberately chooses a consenting friend; no silent/automatic calls.
- **Ludo:** 2, 3 or 4 players; private rooms, online matchmaking, turn indicators, rematches and reconnection.
- **WHOT!:** 2–4 players; private rooms, matchmaking, rematches and reconnection. Select and clearly disclose one consistent rule set before release because local house rules vary.

Guests may practise. Persistent points, rankings, achievements and online multiplayer require an account.

## 3. Player profile and onboarding
First visit: welcome, choose nickname, choose avatar, then guest practice or registration. Public profiles must not require real names or reveal phone numbers. Guest progress may be claimed at registration only through a verified, one-time transfer that prevents duplicate reward claims.

Profile: nickname, avatar, cosmetics, game statistics, achievements, trophies, match history, season ranking and privacy controls.

## 4. Points economy — strictly separate from business promotion
One shared Games Points wallet across entertainment features. Points are not money: no cash-out, sale, transfer for money, betting or staking. Losing a match never removes existing points.

Suggested balancing values (not final): daily quiz completion 20; perfect quiz bonus 30; completed Ludo/WHOT match 10; verified win bonus 15; Daily Arena participation 5; first major milestone 25.

Rewards require genuine participation. Enforce daily caps, deduplicate rewards, detect self-play/fake accounts/repeated farming and keep all balances and reward decisions server-authoritative.

Points may be spent only on entertainment cosmetics and collectibles:
- Avatar outfits, hairstyles and accessories.
- Profile frames, badges, titles and decorations.
- Ludo boards/dice and WHOT! card backs/table themes.
- Emotes and reactions.
- Trophy Room and seasonal cosmetic collections.

Cosmetics must not improve competitive ability.

**Non-negotiable:** Games Points must never buy ads, sponsored listings, business promotion, visibility, views, followers or promotional placement. Never exchange Games Points for business promotion credits. Business promotion remains a separate money-based economy with clear sponsored labels.

## 5. Leaderboards and seasons
Four rankings: Quiz Champion, Ludo Champion, WHOT! Champion and All-Round Champion. Provide Global and Friends views with privacy controls. Use monthly seasons; lifetime trophies remain. Rank by verified skill/performance, not purchases or raw match volume. Provide newcomer opportunities and anti-cheat review.

## 6. Multiplayer fairness and safety
The server validates turns, legal moves, dice randomness, scores, match outcomes and rewards. Add matchmaking, private rooms, invitations, rematches, reconnection and sensible disconnect handling. Report/block tools, moderation, anti-spam and privacy protections are required. Never simulate server-verified features in a local-only client.

## 7. Home integration
Keep the existing home tools intact. Recommended home emphasis: Daily Arena spotlight, then Discover, Games Arena, My Vault, My Diary and Referral Hub. Keep navigation clean and phone-friendly; rotate the Daily Arena spotlight between Hot Seat, Quick Fire, Meme War and a featured game challenge.

## 8. Development sequence
1. Foundation: inspect app architecture, navigation, player identity model, backend trust boundaries and security plan.
2. Daily Arena: Hot Seat, Quick Fire, Meme War, voting/results, reporting and moderation.
3. Profiles and points: wallet ledger, reward rules, achievements, cosmetics and Trophy Room.
4. Big Quiz: practice, daily competition, lifelines and rankings.
5. Ludo: private rooms, matchmaking, authoritative gameplay, reconnection and rematches.
6. WHOT!: rule set, rooms, matchmaking, authoritative gameplay and rematches.
7. Leaderboards/store: global/friends, seasons, cosmetics and collections.
8. Testing/release: guest/account migration, anti-cheat, abuse, privacy, reconnects, Android device testing and verified builds.

## 9. Engineering guardrails
- Preserve Discover, Vault, Diary, Referral Hub and existing utilities.
- Do not expose Supabase service-role keys or trust client-supplied scores/balances.
- Every exposed database table must have appropriate RLS and least-privilege grants.
- Use append-only reward ledger entries and idempotency keys; do not allow clients to update balances directly.
- Build moderation and reporting before public user-generated content.
- Do not launch incomplete multiplayer as live.
- Batch related changes and verify CI/APK artifacts before calling a build successful.
- No production deployment until functional, security and device testing is complete.

## Current implementation status
- Phase 1 home-screen entry for Daily Arena added.
- Phase 1 Daily Arena foundation preview screen added. It clearly identifies future online features as not yet live.
- Existing Supabase database currently contains only the six Discover tables; no entertainment tables have been created.
- No live points, voting, public submissions, profiles, multiplayer, leaderboards or rewards have been enabled yet.
