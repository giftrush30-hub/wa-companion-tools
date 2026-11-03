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
- Authoritative online gameplay for Big Quiz, Ludo and WHOT! (shared state, turn validation, private card hands, reconnect snapshots), plus matchmaking, moderation and rematches.
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


### Games Arena polish pass — 3 November 2026
- **WHOT! table presentation:** Playable cards now use distinct suit-colour treatments (Ball blue, Angle green, Cross purple, Carpet orange, Star gold and WHOT dark navy). Cards that cannot be played are visibly muted and disabled, making the current hand easier to scan.
- **WHOT! exhausted-market rule:** Fixed the no-draw-card dead-end. When the draw pile is empty and the discard pile has no recyclable cards, the round now ends; the player(s) holding the fewest cards win, with a tie explicitly shown. The result can be shared.
- **Big Quiz presentation:** Added a 30-level progress bar, a stronger score/level status line and four distinct answer-button colours. Completion fills the progress bar, and replay resets it.
- These are source updates only. No Android build, device test, APK creation or deployment was performed. The traditional cross-shaped Ludo board and full turn-handoff polish remain outstanding.


### Ludo traditional-board and turn-flow pass — 3 November 2026
- Replaced the custom square-loop visual with a conventional **15×15 cross-shaped Ludo board**: four 6×6 coloured corner yards, a 52-square clockwise shared route, four five-cell private home lanes and a four-colour centre finish.
- Corrected the coordinate sequence to contain all 52 shared track cells and aligned the player start offsets at 0, 13, 26 and 39. Safe cells are represented at indices 0, 8, 13, 21, 26, 34, 39 and 47; captures are prevented on those cells.
- Corrected token progress to the conventional model: positions 0–50 on the shared route, 51–55 in the private home lane, and 56 at the finish. Exact-roll finishing remains required; pair blockades are checked along the shared route.
- Two-player setup now uses opposite corners (Red and Yellow); three-player setup uses three distinct corners; four-player setup uses all four. Added a pass-phone confirmation before the next player rolls, making local hand-off clearer.
- Geometry and rule design were cross-checked against a published 15×15 Ludo implementation and a current Ludo board/rules guide: https://github.com/bocaletto-luca/Ludo and https://passandplaygames.com/guides/ludo. Rule variants still differ between families and apps; this remains the selected practice ruleset.
- This is a source-code update only. No Android build, APK, device test or deployment was performed. The board and interaction still need visual verification on a phone before being called finished.

## WHOT visual card redesign - 2026-11-03
- Replaced plain coloured text buttons with individual card-face components in WhotActivity.kt.
- Card faces use a cream surface, deep-brown corner values, large suit glyphs for Ball, Angle, Cross, Carpet, Star and WHOT, rounded edges, and a consistent suit/value footer.
- Legal cards have a stronger green outline, elevation and PLAY cue. Illegal cards are muted and marked LOCKED. Suit glyphs supplement colour so suit recognition is not colour-only.
- The call card uses the same visual language. The hand is displayed in a two-column touch-friendly grid and remains protected by the pass-phone reveal step.
- Research references: whot.online describes cream cards, deep-brown symbols and mirrored corners; Tabletop Runner and Prototable discuss information hierarchy, visible playability, consistent card layout and readability.
- Visual-only change: no rules, points, online play, build, device test or deployment were added.

## Ludo animation pass - 2026-11-03
- Updated `app/src/main/java/com/example/wacompanion/LudoActivity.kt` with a dedicated animated dice display: rapid face changes, rotation and a brief scale pulse settle on the final roll before the existing legal-move logic runs.
- Token movement now animates along the shared track or into the home lane, then resolves captures, safe squares, bonus turns and victory only after the piece reaches its destination.
- Existing dice rules, token progress, capture rules, pass-phone handoff and offline/no-points boundary are retained.
- Source change committed to GitHub main. No Android build, device test, APK or deployment was run; animation timing and rendering still need on-device review.

## Big Quiz category and show-flow update - 2026-11-03
- Updated `app/src/main/java/com/example/wacompanion/BigQuizActivity.kt` to offer a subject picker before play: All Categories (30), Nigeria & Africa (5), Science & Nature (7), Maths & Logic (9), Digital & Technology (3), World & Society (4), and Business & Money (1).
- The selected subject filters the existing verified-in-source question bank; no question text or answer keys were changed. Category selection resets score, progress and the one-use 50:50 lifeline.
- Added clearer show identity, selected-category label, question numbering, and more prominent question presentation. Results now offer a direct route to choose another category.
- Practice remains offline and awards no Games Points or competitive ranking. No new reward economy or monetization was introduced.
- Research informed the design: Kahoot guidance recommends varied, clearly presented question experiences, difficulty-appropriate timing and concise options; the CDC quiz guidance highlights explanations and useful end-of-quiz feedback. This update does not yet add timers or new question formats.
- Source committed to GitHub main. No build, APK, device test or deployment performed.

## Big Quiz question-bank expansion — 3 November 2026
- Expanded the offline practice bank from **30 to 66 questions**. Each new round shuffles question order and independently shuffles the four answer choices, remapping the correct-answer index so the answer key remains correct. Replays offer a fresh sequence instead of repeating a fixed order. The full challenge now contains 66 questions; the final-level banner follows the selected challenge length rather than assuming the last level is always question 30.
- All seven subject choices remain available. The category pools now contain: Nigeria & Africa (11), Science & Nature (13), Maths & Logic (15), Digital & Technology (9), World & Society (10), and Business & Money (7), plus the full 66-question challenge.
- Added questions on Nigerian and African civics/geography, core science, arithmetic and logic, digital literacy and online safety, world knowledge, and practical business/financial literacy.
- Added concise explanations to each new question. Result tiers now use percentage accuracy, so the same performance standards apply fairly to short subject rounds and the 66-question full challenge. No timers, points, paid rewards, ranking or network competition were introduced; practice remains offline and free of Games Points.
- Content was designed around the quiz-learning patterns already reviewed from Kahoot and CDC guidance: clear question wording, manageable answer choices, useful explanations and completion feedback. This is original WA Companion question content, not copied quiz material.
- Source-only change committed to GitHub main. No Android build, APK, device test or deployment was performed.

## Games Arena reliability and hub copy pass — 3 November 2026
- Updated the Games Arena hub to accurately describe Big Quiz as a 66-question offline practice bank across six subjects, with a subject choice or full challenge. The old 30-question description was stale.
- Hardened WHOT draw-pile exhaustion handling. Drawing now reports whether the requested cards were successfully drawn; if the market and recyclable discard are exhausted, the round ends immediately and the calling action stops rather than advancing the turn or continuing a General Market distribution after the result screen appears.
- This preserves the chosen local WHOT practice rules and does not introduce points, rankings or online play.
- Rule references remain the Nigerian WHOT variant documented by Pagat (https://www.pagat.com/com/whot.html) and conventional Ludo safe-square, blockade and exact-finish rules (https://leludo.org/rules). These sources inform rule consistency; local house-rule variations are still disclosed in the game.
- Source-only changes committed to GitHub main. No Android build, APK, device test or deployment was performed.

### Games Arena progression improvement — 3 November 2026
- Big Quiz now stores each category's personal-best correct-answer count locally on the device, plus a total completed-round count. These survive app restarts on the same installation.
- Added four one-time local achievements: First Finish (complete a round), Expert Mind (75%+), Quiz Champion (90%+) and Perfect Round (100%). Newly earned achievements appear on the result screen alongside the personal best and overall achievement count.
- This is offline practice progression only. It does not award, spend or affect Games Points, paid promotion, public rankings or competitive claims. Local records are not synced across devices or accounts.
- Product pattern: Kahoot's guidance around replay variety and learner feedback informed the use of persistent personal improvement signals rather than rewarding raw volume. Keep progression transparent and avoid pay-to-win benefits.
- Not build-tested or device-tested; no APK or deployment was produced.

### Ludo and WHOT local match records — 3 November 2026
- **Ludo:** after a completed victory, the result screen now records the number of completed matches and victories on that device, displays a first-victory / victory-builder milestone, and shows up to five recent results. The log is local to the installation.
- **WHOT!:** both a normal first-to-empty-hand win and an exhausted-market result are recorded. The result panel displays completed matches, player wins, first-match / first-win milestones, and up to five recent results. A tied exhausted-market result is recorded as a completed match but does not inflate the win count.
- These are private, local practice records only. No Games Points, cash value, paid promotion, online leaderboard or account sync is involved. Names shown in these local pass-and-play logs are generic seat labels (e.g. PLAYER 1 / RED), not account identities.
- This batch extends the Big Quiz personal-best and achievement pattern to the other two offline games. It does not claim online multiplayer or server-verified results.
- Static source inspection only; no Android build, APK, device test or deployment was performed.

## Games Arena multiplayer direction — 3 November 2026
- **Arena Party is explicitly excluded.** There is no separate social lounge or standalone voice room. Room chat and optional live voice are attached to the selected game room.
- Target experience: members create private invite-code rooms or join public rooms; each room has player presence, ready state, game-specific limits, in-match chat, optional voice, rematches and reconnection.
- Multiplayer scope: Big Quiz (shared questions, timer and score); Ludo (authoritative dice, token moves, captures, turns and reconnect state); WHOT! (private hands, shared deck/discard, special-card effects and turn validation).
- Existing offline Big Quiz, Ludo and WHOT! practice remains available and must not be broken by online mode.
- **Live Supabase foundation:** migration `20261103205537_games_multiplayer_rooms_chat_tables` is recorded in the connected project's migration ledger. Tables `game_rooms`, `game_room_members` and `game_chat_messages` have RLS enabled. Private room/member/chat reads are scoped to active members; signed-in users can discover public waiting rooms. Chat is limited to 300 characters. Room RPCs check authentication, game type, room capacity, room code, ready state, host authority and host hand-off. Anonymous execution is revoked.
- Room codes are eight uppercase hexadecimal characters. Ludo/WHOT! support 2–4 players; Big Quiz supports 2–8.
- Private Realtime authorization policies are live and scoped to active room members for Broadcast/Presence on topic `room:<room-uuid>`. Realtime's project-wide private-channel enforcement setting has not yet been verified.
- **Android room and chat UI added:** `GamesMultiplayerActivity.kt` provides private/public room creation, room-code joining, public room discovery, roster/readiness display, invite sharing, persistent text chat, host start and leave. The same room chat/voice screen can be reopened from each of the three game activities. The roster and chat history also refresh periodically as a fallback to Realtime events.
- **Android Realtime client added:** `GamesRealtimeClient.kt` uses a private Supabase WebSocket channel for room presence and broadcast, with heartbeat, session token refresh and reconnect backoff. `GamesMultiplayerBackend.kt` handles authenticated room operations and persistent chat.
- **Voice endpoint deployed:** Supabase Edge Function `game-voice-token` is ACTIVE (version 2, JWT verification enabled). It verifies the caller, active room membership and in-progress room status before signing a two-hour, microphone-only LiveKit token. The Android app has a LiveKit voice client, runtime microphone permission request, join/leave, microphone mute and speaker mute controls.
- **Voice is not operational until LiveKit Cloud credentials are set as Supabase Edge Function secrets:** `LIVEKIT_URL`, `LIVEKIT_API_KEY`, and `LIVEKIT_API_SECRET`. These values must never be committed or placed in the Android app.
- **Critical gameplay limitation:** the lobby currently starts the existing local game activity on each phone and provides room chat/voice access. It does **not yet synchronize** Ludo dice/token moves, WHOT! private hands/card actions, or Big Quiz question/timer/score state across phones. Do not market or describe the games as fully playable online until the authoritative multiplayer game engines are implemented and tested.
- Remaining work: authoritative game-state protocol for all three games; secure player action validation and reconnect snapshots; chat reporting/blocking/moderation; rematch flow; realtime private-channel setting verification; LiveKit secret configuration; two-device tests on real networks; and Android build verification.
- No Games Points, business visibility, paid promotion, cash prizes or betting are attached to multiplayer rooms.

### Multiplayer implementation checkpoint — 3 November 2026
- Live Supabase migration ledger entry: `20261103205537_games_multiplayer_rooms_chat_tables`.
- GitHub commits:
  - Migration source: `cd8af8d9eaecc4d8d253dd7cfe05b2d06ef96eb2`, subsequently aligned with live RPCs in `10171ad0e3ef16a41e244c36f8c6cf1d31b07e4f`.
  - Room/chat REST backend: `e531e73770a516741fcad20bb774299f9c4d004f`, with saved-message return support in `24dd3c730ce8b3ed3d644e3007054467c3218c71`.
  - Shared Supabase config visibility for Realtime: `15d193af4a9e3091859f2592d82f9248bcee4e9f`.
  - Realtime WebSocket client: `7ad5f649492642be269868ecc8a7cbffd61396b0`; OkHttp dependency: `07d91b9ae023bac9095a3413f6d2af92379f86aa`.
- Important: these are source/backend foundations only. The online games are not yet exposed in the Games Arena UI and cannot yet be played as synchronized matches. Voice is not live until LiveKit credentials, a secure token endpoint, Android microphone UI and runtime tests are in place.

- Voice SDK references checked: LiveKit Android 2.30.0 room connect/microphone/speaker mute API, and LiveKit's requirement that room access tokens be generated by a trusted backend. The Edge Function is deployed and active, but voice remains unavailable until LiveKit Cloud URL/API key/API secret are configured as Supabase function secrets.

- Additional implementation commits:
  - Correct private Realtime topic policy parsing: `dc2da50b0189263a014b09db0e472966cad4e485`.
  - Multiplayer room/chat backend: `e531e73770a516741fcad20bb774299f9c4d004f`; return saved chat row for broadcast: `24dd3c730ce8b3ed3d644e3007054467c3218c71`.
  - Realtime socket: `7ad5f649492642be269868ecc8a7cbffd61396b0`; OkHttp: `07d91b9ae023bac9095a3413f6d2af92379f86aa`.
  - Voice token endpoint: `a8f6e7bdf3e1c64398d3e9e4609bf2701c412e36`, publishable-key fallback: `d5614976624713ed8ae16dd4c7322a901e30457c`, microphone-only token scope: `4856dc18bb794cacffa6fdfc9a40e68b1cc8e275`.
  - Voice token Android client: `760658b8b5c5a3611933a32d3ddc9eae2e6f08ca`; LiveKit voice client: `e7a02df2fc37e26de6c966af7ba2b2c1cd3b9928`; LiveKit SDK/JitPack: `3e28e35ee3493aa27659140311f3b305a39246c3`, `67643b2a0f5ca6564bf3c5e789e045eb94b4cbb2`; microphone permission: `95e83b9aafaa61d197b76cd63a3d1d9bf6287150`, speaker routing permission: `ba43cef590a41f073ae3b278b9a1336157aba13d`.
