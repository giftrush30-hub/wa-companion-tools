# WA Discover — WhatsApp-first business discovery

WA Discover is a Nigeria-wide discovery directory inside WA Companion. It helps customers find businesses that already sell through WhatsApp. It is not a modified WhatsApp app and does not copy or read WhatsApp Status automatically.

## Confirmed product direction

- Sellers continue using WhatsApp and WhatsApp Business as they do today.
- Sellers share their business details, WhatsApp contact link and WhatsApp Business catalogue link with WA Discover.
- Buyers browse businesses by keyword, category, state and city, then open the seller's catalogue or WhatsApp chat.
- WA Discover stores directory information and links, not copies of every product photo or video.
- Basic business discovery is intended to be free.
- Free basic listings remain available to approved sellers. Optional paid promotions are priced as listed below and must be clearly labelled **Sponsored**.
- The initial launch audience is all Nigeria, with location filters.
- Buyers should be able to browse without creating an account.
- Sellers create a secure account to submit and manage their directory entry; new entries remain pending until reviewed.

## Advertising packages and Paystack preparation

The agreed introductory package menu is:
- **Free Basic Listing — ₦0:** standard approved directory listing.
- **Local Boost — ₦50:** sponsored placement for local discovery inside WA Discover.
- **Wider Discovery — ₦100:** sponsored placement across wider WA Discover discovery.
- **Featured Listing — ₦300:** more prominent Sponsored placement inside discovery.
- **Business Spotlight — ₦1,000:** highest-visibility Sponsored placement inside WA Discover.

These prices are the agreed starting menu, not guaranteed reach or sales. No campaign duration or impression guarantee has been promised yet; do not invent either. Paid placements must be labelled Sponsored. They do not buy WhatsApp Status views, access private contacts, or authorize unsolicited messages.

The Android screen now displays these packages and a Paystack-coming-soon notice. It does not collect money or start a campaign yet. The SQL schema prepares a public package catalogue and private promotion-order records. Before real checkout, a trusted server must create the Paystack transaction, verify the signed webhook and transaction amount/reference, and only then activate the promotion. Never trust an Android success screen or a client-submitted payment status. Keep the Paystack secret key on the server only.

## Important WhatsApp limitations

- The app cannot secretly read a person's WhatsApp Status, private chats, contacts or catalogue.
- Sellers must deliberately submit their business details and share their catalogue/contact links.
- A WhatsApp Business catalogue link can let buyers view products hosted by WhatsApp, subject to the seller's account and WhatsApp's own availability.
- WA Discover can measure its own directory impressions, listing opens and outbound WhatsApp/catalogue taps. These are not WhatsApp Status views and do not guarantee enquiries or sales.
- Never promise a specific number of views or sales.

## Lean first-version data

The first shared directory stores only:
- Business name and short description
- Category
- State and city
- International WhatsApp number
- Optional WhatsApp Business catalogue URL
- Owner account ID, approval status and timestamps
- Simple directory engagement events and abuse reports

No product photo/video uploads, product listings or payment processing are needed in the first directory release. Promotion package definitions and payment-order preparation are now included, but checkout remains disabled.

## Backend schema status

The current `supabase/schema.sql` includes `discover_categories`, `discover_businesses`, `discover_reports`, `discover_events`, `discover_promotion_packages` and `discover_promotion_orders`.

Public users can read approved business profiles and active categories. Sellers need an authenticated account to submit a pending business and can edit their own details. A database trigger prevents ordinary sellers from changing ownership or approval status. Admin authorization uses a trusted Supabase `app_metadata.role = discover_admin` claim; it must only be granted through trusted server-side admin tooling, never user-editable metadata.

**The initial schema has now been applied to the connected Supabase project** (`coodsyfdhjpqfxanqhwm`). Tables and RLS policies were confirmed in the project. The admin helper function search path and foreign-key indexes were also hardened. The Android app now connects to Supabase using a client-safe publishable key. Buyer keyword/state/city search and seller sign-up/sign-in plus pending listing submission are implemented in source. The RLS performance policies were optimized; the current security advisor returns no findings. Physical-device and end-to-end tests are still required. Do not paste service-role keys into the app or GitHub.

## Current milestone

The Android screen now has live approved-business search, seller account creation/sign-in, seller listing submission as pending review, local draft saving, and WhatsApp/catalogue hand-off. The latest seller-flow build is running in GitHub Actions; do not call it verified until that run succeeds. Physical-device and backend end-to-end testing remain outstanding.

## Next implementation stages

1. Confirm the latest Android build and test seller registration/submission on a real phone.
2. Add real category filtering and session refresh/sign-out handling.
3. Add reporting, moderation and abuse controls.
4. Test public search, seller ownership and approval restrictions end to end.
5. After real buyer activity exists, enable Sponsored packages with server-verified Paystack payments.

Never put a Supabase service-role key or payment secret in Android source code or GitHub. Do not charge sellers before there is real buyer traffic.

## Build and testing

A successful GitHub Actions Android build verifies compilation and artifact creation only. It does not prove that the backend is connected or that the directory works on a physical phone. Test seller submission, public browsing, moderation and outbound links before public launch.


## Buyer link hand-off (implemented in Android UI)

The current Android screen includes two direct buyer actions:
- **Open a WhatsApp catalogue link**: the buyer pastes a link intentionally shared by a seller; Android opens it with the installed app or browser.
- **Chat with a business on WhatsApp**: the buyer enters an international phone number; the app opens the wa.me chat link. For Nigeria, enter 234 followed by the national number without its leading zero.

These are link hand-off tools, not public discovery search. The public searchable directory is connected to Supabase and returns approved listings only. The app must not claim that a pasted link is verified or that a WhatsApp message was sent; the buyer reviews and sends any message in WhatsApp themselves.


## WA Companion expansion and simplicity rules (2026-11-03)

The home screen is being simplified around four clear destinations: Discover, My Vault, My Diary and Referral Hub. Existing utilities move behind a single More tools action so first-time users are not confronted with a long catalogue of features.

Design rules are based on Android's current navigation guidance:
- Keep only a small number of primary destinations visible (3–5).
- Use clear labels and familiar icons; do not rely on icons alone.
- Give each screen one obvious main action.
- Reveal advanced options only when needed.
- Keep language short, friendly and non-technical.
- Keep forms short and explain why information is needed.
- Never put paid promotions, points or advanced settings in the user's way when they are trying to complete a simple task.

### Personal space milestone
- My Vault and My Diary screens have been added as a first simple implementation.
- Current version saves text notes/entries locally on the phone only.
- It does not yet upload files, sync across devices, back up to Supabase or provide cloud account recovery.
- File attachments, encrypted cloud storage, import/export and cross-device sync are later stages. Do not describe local notes as cloud-backed.

### Referral Hub milestone
- A first simple Referral Hub draft screen has been added.
- Referral submissions currently save as local drafts on the phone only; they are not public, reviewed, promoted or visible to other members.
- Public referral listings, link safety checks, moderation, paid campaigns, points boosts and verified analytics require a secure shared backend and are not active yet.
- No external clicks, registrations, sales or affiliate conversions are guaranteed.

### Message archive and WhatsApp integration boundaries
- Future chat archive should use files deliberately exported/shared by the user or individual messages the user chooses to share.
- It must not scrape private WhatsApp chats, contacts, groups, channels or Status.
- It must not automate unsolicited WhatsApp messages, follows or group joins.
- Users remain in control of opening WhatsApp links and taking actions inside WhatsApp.

### Current development status
- The home navigation, local Vault/Diary, and local Referral Hub draft screen are committed to main.
- The last known Android build before these changes failed on a Kotlin inputType assignment in the seller form; that issue has been corrected in source. The newest GitHub Actions run must pass before the APK is considered ready.
