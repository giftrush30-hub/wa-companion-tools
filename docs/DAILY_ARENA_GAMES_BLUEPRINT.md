# WA Companion — Daily Arena & Games Blueprint

Status: Product blueprint and Phase 1 foundation. This document is the source of truth for the entertainment roadmap.

## Product vision
Add an engaging youth entertainment experience without replacing or damaging Discover, My Vault, My Diary or Referral Hub. Daily Arena provides lightweight daily participation; Games Arena provides skill-based and social games.

## 1. Daily Arena
A home-screen spotlight leads to:
- **Hot Seat:** daily opinion questions and polls, community-submitted questions with approval and upvoting, results, reporting and moderation.
- **Quick Fire:** short timed quizzes across general knowledge, Nigerian culture, entertainment, sports, technology, business, logic and education; difficulty modes and answer streaks.
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
- **Hot Seat:** 36 published polls across Life & Growth, Naija Life, Business & Money, Entertainment, Relationships, Tech & Future, Community, Lifestyle, Learning, Naija Banter, Thought Provoker, Character, Digital Life and other everyday topics. Polls rotate randomly and the client remembers recently shown polls to reduce repeats. Players can skip to another question, results show vote totals and percentages, and reporting/moderation are available. Supports poll, this-or-that and hot-take labels. Players can suggest questions with 2–4 options; suggestions are reviewed before publication, limited to three per user per 24 hours, and the community can upvote each approved suggestion once. Trusted admins have a separate review queue.
- **Quick Fire:** 175 questions across ten published packs: Naija Mix, Naija & Culture, Tech & Digital Life, Science & The World, Entertainment & Sport, True or False, Money & Business, Words, Logic & Everyday Skills, Naija Brain & Banter, and Street Smart & Big Brain. Players choose Mixed (10 questions), Easy, Standard or Expert (5 questions each). Each round randomly selects a pack, shuffles questions and answers, and avoids recently played question IDs on the device. Timers are 25 seconds for Easy, 18 for Mixed/Standard and 12 for Expert; consecutive correct answers build a visible streak. Explanations and one-answer-per-question protection remain active. Quiz score and streak are session-only, not Games Points.
- **Meme War:** 25 published prompt challenges across everyday Naija, money, family, city life, work, lifestyle, tech, food and friends. Players can submit one caption per challenge, optionally attach a photo (up to 5 MB; JPG/PNG/WEBP), and add fourteen image-based Twemoji stickers. Eight prompts now include real photo templates with creator credits. Entries remain pending review until approved. Approved photo/caption posts are ranked by community votes; each player can vote once per caption and cannot vote for their own entry. Users can report captions.
- **Moderation and safety:** moderator queue and approve/reject controls for Meme War captions, plus a Daily Arena report review queue (reviewing/resolved/dismissed), are implemented in the Android source. The database checks the trusted `app_metadata.role = 'admin'` claim for moderator access. Assign an admin role only through a trusted Supabase admin process; never use editable user metadata for this.
- Meme War photo upload and display now use a dedicated public Supabase Storage bucket with authenticated per-user upload folders. The current sticker row uses Twemoji artwork rendered as images and inserts the corresponding emoji into captions. Meme War now composites the caption onto the selected photo or challenge template before upload, with Classic/Yellow/Clean text styles and top/bottom placement controls. The sticker library has fourteen image-based options. A curated original sticker pack and full visual redesign remain future visual work.
- Supabase now contains Hot Seat, Quick Fire, Meme War and reporting tables, with RLS and server-side checks for the implemented features.
- **Daily missions and history:** private activity events are logged by database triggers for Hot Seat votes, Quick Fire answers, Meme War captions and Meme War votes. Daily Arena displays four participation missions plus a private seven-day activity summary. These missions do not award Games Points.

### Not yet implemented
- Shared Games Points wallet, reward ledger, cosmetics store, achievements and Trophy Room.
- Big Quiz competition mode and lifelines.
- Ludo and WHOT! authoritative multiplayer, rooms, matchmaking and reconnection.
- Public profiles, seasons and leaderboards.
- Full image templates, stickers, richer visual assets and interface redesign.
- Final device testing and production release.



### Daily Arena sharing and content expansion — 3 November 2026
- Android share-sheet actions generate actual 4:5 PNG image cards for Hot Seat questions/results, Quick Fire questions/results, Meme War challenge prompts and approved Meme War captions. The system chooser lets players select WhatsApp, WhatsApp Status where supported, or another installed sharing app. Cards do not expose private vote identities or player account details.
- Added **Quick Fire: Naija Brain & Banter**, a new published pack with 30 questions: 10 easy, 10 standard and 10 expert. The pack mixes practical judgement, everyday Nigerian humour, media literacy, money reasoning, critical thinking and decision-making. Questions aim to be funny without humiliating people or relying on stereotypes.
- Live Supabase inventory check confirmed 30 questions in the new pack with the intended 10/10/10 difficulty split. Total published Quick Fire content was later expanded to 175 questions across ten packs, as confirmed by a fresh live inventory query on 3 November 2026.
- Product research for this batch: Kahoot's official question-type guide supports mixing knowledge checks with opinion-gathering and open-ended formats; Slido highlights word clouds, open text, ranking polls, audience Q&A and moderation; Wayground (Quizizz) describes memes and multiple interactive question types as engagement tools. We are applying the general interaction patterns, not copying their branding or assets.
- The share-card renderer is implemented in `ArenaShareCard.kt`; it creates local PNG artwork and shares it using AndroidX FileProvider content URIs. This source change has not yet been build-tested or verified on a physical device.

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


### Daily Arena improvement batch — 3 November 2026
- Added community Hot Seat question suggestions, moderator approval, one-support-vote-per-user, aggregate support counts and a three-suggestions-per-user daily rate limit. Suggested questions are not auto-published.
- Quick Fire now has Easy/Standard/Expert/Mixed modes, server-tagged question difficulty, countdown timers, correct-answer streaks, and local recent-question rotation to reduce repetition. These streaks are entertainment feedback only; no Games Points are awarded yet.
- Meme War now supports Classic, Bold Yellow and Clean caption styles, top/bottom caption placement, fourteen Twemoji sticker images, and three additional photo-backed prompts.
- Product research: Kahoot official question types and randomization informed quiz mode/timer design; Slido's official Q&A product and moderation guides informed community suggestions, upvoting and pre-publication review; Unsplash free photo pages were checked for creator and license attribution. See references below.
- Current published backend inventory before the latest expansion: 16 Hot Seat polls, 145 Quick Fire questions in 9 packs, 15 Meme War challenges, 8 photo-backed Meme War templates.


### Daily Arena improvement batch — 3 November 2026
- Added community Hot Seat question suggestions, moderator approval, one-support-vote-per-user, aggregate support counts and a three-suggestions-per-user daily rate limit. Suggested questions are not auto-published.
- Quick Fire now has Easy/Standard/Expert/Mixed modes, server-tagged question difficulty, countdown timers, correct-answer streaks, and local recent-question rotation to reduce repetition. These streaks are entertainment feedback only; no Games Points are awarded.
- Meme War now supports Classic, Bold Yellow and Clean caption styles, top/bottom caption placement, fourteen Twemoji sticker images, and three additional photo-backed prompts.
- Daily Arena now records private activity events from server-side triggers and shows four daily missions plus a seven-day participation history. The history is private to each account and does not award Games Points.
- Product research: Kahoot official question types and randomization informed quiz mode/timer design; Slido's official Q&A product and moderation guides informed community suggestions, upvoting and pre-publication review; Unsplash free photo pages were checked for creator and license attribution. See references below.
- Current published backend inventory: 36 Hot Seat polls, 175 Quick Fire questions in 10 packs, 25 Meme War challenges, 8 photo-backed Meme War templates.


### Visual image sharing expansion — 3 November 2026
- Replaced text-only share actions with generated 4:5 PNG image cards using the Android Sharesheet and FileProvider content URIs.
- Hot Seat shares a designed question card with answer options; after voting it can share a separate results card with community percentages.
- Quick Fire shares a designed challenge card containing the question and options, plus a result card with score and best streak.
- Meme War shares the challenge as a designed image card and embeds its actual challenge photo when available. Approved caption shares embed the submitted meme image when available, alongside the caption and vote context.
- Images are generated locally, saved temporarily under the app cache shared_memes/ directory, and exposed through the existing non-exported AndroidX FileProvider with read grants. The existing @xml/file_paths already grants the matching cache subdirectory.
- Users can choose WhatsApp, WhatsApp Status where supported, or another compatible app. The output is a real image attachment rather than a text-only message. A remote photo failure falls back to a designed graphic card.
- Inspiration: Kahoot documents a mix of knowledge checks and opinion-gathering formats; audience participation and visualized responses informed the share-card framing. These are original WA Companion graphics, not copied assets.
- Not verified on-device and not build-tested in this batch. No APK was produced and no deployment was made.


### Hot Seat content expansion — 3 November 2026
- Added 20 additional published Hot Seat prompts through the idempotent migration `20261103140000_hot_seat_fun_thoughtful_question_expansion.sql`.
- The new batch covers funny everyday Naija moments, money decisions, relationships, character, digital habits, teamwork and thought-provoking future-self questions. It includes multiple poll styles: standard polls, hot takes and this-or-that dilemmas.
- New questions use light humour without targeting protected groups, humiliating individuals or encouraging unsafe behaviour. Options are designed to prompt conversation rather than imply one universally correct answer.
- Live Supabase query returned all 20 newly inserted prompts and their option rows. The prior published inventory was 16, so the intended combined inventory is now 36 published Hot Seat polls.
- Inspiration: Kahoot's official catalogue distinguishes knowledge testing from opinion collection and offers formats including polls, word clouds, open-ended answers, puzzles and sliders; Slido offers multiple-choice, word cloud, rating, open-text and ranking polls. Daily Arena is applying the underlying variety principles. Word cloud, ranking and open-text modes are future work; they are not represented as live features yet. https://support.kahoot.com/hc/en-us/articles/115002308428-Kahoot-question-types and https://www.slido.com/product?lang=en


### Daily Arena content and Games Arena first playable milestone — 3 November 2026
- Live Supabase inventory was queried after the latest migrations: **36 published Hot Seat polls**, **175 Quick Fire questions across 10 published packs**, **175 private answer keys**, and **25 published Meme War challenges**.
- Hot Seat gained 20 new prompts across Naija banter, money decisions, relationships, character, digital habits, teamwork and reflective dilemmas.
- Quick Fire gained the 30-question **Street Smart & Big Brain** pack (10 easy, 10 standard, 10 expert). The server keeps the answer keys private and checks submitted answers. The live inventory query confirmed all 175 published questions have corresponding private answer keys.
- Meme War gained 10 clean, relatable everyday prompts around transport, data, work, family chat, food, small business and phone habits.
- **Games Arena has now started:** a Games Arena hub is linked from the home screen. Big Quiz has a first offline practice round with 15 curated questions, a one-use 50:50 lifeline, explanations, result feedback and visual image sharing. This is practice only: no Games Points, online ranking or competitive result is claimed.
- Ludo and WHOT! remain in development. Their online matchmaking, private rooms, turn validation, reconnects, and rematches must be server-authoritative before being advertised as playable multiplayer.
- The first Big Quiz practice content is currently bundled locally in the Android activity. It is not yet a server-managed daily competition or persistent player record.
- No build verification, APK production, device test or deployment was performed for this batch.


### Games Arena build kickoff — 3 November 2026
- Added a Games Arena entry to the app home and a dedicated Games Arena hub screen.
- Added the first playable **Big Quiz practice** activity: 15 curated questions, an ascending question ladder, one 50:50 lifeline per round, immediate answer explanations, a result summary and image-based sharing.
- Practice is offline and deliberately does not award Games Points, affect competitive rankings or claim server-verified results. The question bank is currently bundled in the Android activity; moving it to a managed server bank and adding daily competitive play are later steps.
- Ludo and WHOT! are visible as planned game tracks, not falsely advertised as playable online multiplayer.
- Research for WHOT!: Pagat's rules reference documents the Nigerian card set and explains that some special-card effects vary by local play. Current Nigerian game apps also expose different default action-card and stacking settings. Therefore, the game must show its selected rules before a match and should allow room hosts to see the rule set before inviting others. References: https://www.pagat.com/com/whot.html and https://play.google.com/store/apps/details?id=com.tonielro.naijawhotfree
- Research for Ludo: Leludo's rules guide describes safe squares, blockades, captures and exact-roll finishing; these are useful baseline rules to formalise for our board, while the server must still validate every move and dice result. Reference: https://leludo.org/rules
- This is a source-code milestone only. No build verification, APK, device test or deployment was performed.


### Games Arena playable local milestone — 3 November 2026
- **Ludo:** Added an offline 2–4 player pass-and-play activity. It includes a dice roll, four tokens per player, six-to-leave-base, a 52-square outer loop, four coloured home lanes, safe start squares, captures, pair blocking, exact-roll finishing, bonus rolls for sixes/captures/home and a three-consecutive-sixes turn forfeiture. It does not claim online multiplayer, rankings or Games Points.
- **WHOT!:** Added an offline 2–4 player pass-and-play activity using a 54-card Nigerian deck: 12 Ball, 12 Angle, 9 Cross, 9 Carpet, 7 Star and 5 WHOT cards. The selected table rules are disclosed in the game: 1 Hold On; 2 Pick Two (stackable); 5 Pick Three (stackable); 8 Suspension; Star 8 skips two players; 14 General Market (other players draw one and the player continues); WHOT 20 calls a shape. Drawing ends the turn, and emptying the hand wins. The first face-up card is a non-wild, non-instruction card. Last-card calls are automatic rather than requiring verbal penalties.
- These local versions are intentionally separate from online play. Private rooms, matchmaking, reconnects, server-authoritative dice/card shuffles and match validation remain future work.
- Ludo rule research used a conventional reference for six-to-leave-base, safe squares, captures, exact-roll home and blockades, while noting that Ludo house rules vary. WHOT rule research used Pagat's Nigerian variant guide, which documents the 54-card deck and special-card variants. Sources: https://leludo.org/rules and https://www.pagat.com/com/whot.html
- The app's Games Points remain unavailable in these local games. Points cannot buy business promotion, ads, views, followers or visibility.


### Games Arena follow-up — Big Quiz expansion
- Big Quiz offline practice now has 30 questions, five named progress checkpoints (Starter, Skilled, Sharp, Master and Expert) and a final Quiz Champion level. These are practice milestones only, not paid prizes or Games Points.
- The Ludo activity currently uses a custom square-loop 52-cell practice board with four coloured home lanes. It is a playable local rules foundation, not yet the polished traditional cross-shaped board or an online multiplayer build. The next Ludo pass should improve the board geometry and add a clear turn hand-off/animation treatment before network play.
- The WHOT! activity implements the 54-card deck and the chosen special-card variant described above. House rules vary, so the selected rule summary is shown in-game rather than pretending there is only one universal version.
