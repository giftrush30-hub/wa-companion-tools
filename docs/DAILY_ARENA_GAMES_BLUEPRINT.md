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

## Current implementation status (updated 3 November 2026)

### Live backend foundations and Android entry points
- Daily Arena is connected from the existing app; existing Discover, Vault, Diary and Referral Hub areas remain separate.
- **Hot Seat:** 16 published polls across Life & Growth, Naija Life, Business & Money, Entertainment, Relationships, Tech & Future, Community, Lifestyle and Learning. Polls rotate randomly, players can skip to another question, results show vote totals and percentages, and reporting/moderation are available. Supports poll, this-or-that and hot-take labels.
- **Quick Fire:** 85 questions across six published packs: Naija Mix, Naija & Culture, Tech & Digital Life, Science & The World, Entertainment & Sport, and True or False. Each round randomly selects a pack, shuffles question order, selects up to 10 questions and shuffles answer order while preserving server-side answer checking. Explanations and one-answer-per-question protection are active. Quiz score is currently a session score only; it is not Games Points.
- **Meme War:** 12 published prompt challenges across everyday Naija, money, family, city life, work, lifestyle and tech. Players can submit one caption per challenge, optionally attach a photo (up to 5 MB; JPG/PNG/WEBP), and add eight image-based Twemoji stickers. Five prompts now include real photo templates with creator credits. Entries remain pending review until approved. Approved photo/caption posts are ranked by community votes; each player can vote once per caption and cannot vote for their own entry. Users can report captions.
- **Moderation and safety:** moderator queue and approve/reject controls for Meme War captions, plus a Daily Arena report review queue (reviewing/resolved/dismissed), are implemented in the Android source. The database checks the trusted `app_metadata.role = 'admin'` claim for moderator access. Assign an admin role only through a trusted Supabase admin process; never use editable user metadata for this.
- Meme War photo upload and display now use a dedicated public Supabase Storage bucket with authenticated per-user upload folders. The current sticker row uses Twemoji artwork rendered as images and inserts the corresponding emoji into captions. A curated original sticker pack, caption-on-template editor, decorative text controls and full visual redesign remain future visual work.
- Supabase now contains Hot Seat, Quick Fire, Meme War and reporting tables, with RLS and server-side checks for the implemented features.

### Not yet implemented
- Shared Games Points wallet, reward ledger, cosmetics store, achievements and Trophy Room.
- Big Quiz competition mode and lifelines.
- Ludo and WHOT! authoritative multiplayer, rooms, matchmaking and reconnection.
- Public profiles, seasons and leaderboards.
- Full image templates, stickers, richer visual assets and interface redesign.
- Final device testing and production release.

### Engineering guardrails
- Points remain a separate entertainment-only economy and must never buy business ads, sponsored listings, visibility, views or followers.
- Never expose Supabase service-role keys or trust client-supplied scores/balances.
- All reward decisions must be server-authoritative, capped and idempotent.
- Public user-generated content must pass moderation; reports must be reviewable.
- Do not launch incomplete multiplayer as live.
- Batch related changes; do not spend deployment/build credits on every tiny visual adjustment.
- No production deployment until functional, security and device testing is complete.

### Product research and borrowed design patterns
- Kahoot! (official help): randomized question order and answer order; multiple formats such as quiz, true/false, type-answer, puzzle and polls. Daily Arena applies random pack/question/answer selection now; advanced formats can be added without copying Kahoot's branding or assets. https://support.kahoot.com/hc/en-us/articles/27877912748051-How-to-randomize-questions-for-Kahoot-games and https://support.kahoot.com/hc/en-us/articles/115002308428-Kahoot-question-types
- Slido (official product/help): multiple poll types, audience Q&A, upvoting and pre-publication moderation. Hot Seat uses rotating topics, poll categories, skip/next flow, aggregate results and reporting/moderation. https://www.slido.com/product?lang=en and https://community.slido.com/q-a-settings-222/use-moderation-and-manage-audience-questions-477
- Instagram community safety guidance: built-in reporting and action on violating posts/captions. Meme War keeps pre-approval, reporting and moderation before content appears publicly. https://archive.is/66bu1
- Supabase security documentation: public-schema tables need RLS and least-privilege grants; Storage upload access should be constrained by bucket and authenticated user folder. https://supabase.com/docs/guides/database/postgres/row-level-security and https://supabase.com/docs/guides/storage/security/access-control

- Twemoji artwork: Twitter and contributors, graphics licensed CC BY 4.0. Attribution is shown in the Meme War sticker picker and recorded here. https://github.com/twitter/twemoji
