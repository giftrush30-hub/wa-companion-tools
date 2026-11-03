# WA Discover — WhatsApp-first business discovery

WA Discover is a Nigeria-wide discovery directory inside WA Companion. It helps customers find businesses that already sell through WhatsApp. It is not a modified WhatsApp app and does not copy or read WhatsApp Status automatically.

## Confirmed product direction

- Sellers continue using WhatsApp and WhatsApp Business as they do today.
- Sellers share their business details, WhatsApp contact link and WhatsApp Business catalogue link with WA Discover.
- Buyers browse businesses by keyword, category, state and city, then open the seller's catalogue or WhatsApp chat.
- WA Discover stores directory information and links, not copies of every product photo or video.
- Basic business discovery is intended to be free.
- Optional paid promotion may be added later and must be clearly labelled **Sponsored**.
- The initial launch audience is all Nigeria, with location filters.
- Buyers should be able to browse without creating an account.
- Sellers will need a secure way to register and manage their directory entry.

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

No product photo/video uploads, product listings, paid promotions or payment processing are needed in v1.

## Backend schema status

The current `supabase/schema.sql` has been simplified for this directory model:
- `discover_categories`
- `discover_businesses`
- `discover_reports`
- `discover_events`

Public users can read approved business profiles and active categories. Sellers need an authenticated account to submit a pending business and can edit their own details. A database trigger prevents ordinary sellers from changing ownership or approval status. Admin authorization uses a trusted Supabase `app_metadata.role = discover_admin` claim; it must only be granted through trusted server-side admin tooling, never user-editable metadata.

**This SQL is a reviewed project file only. It has not been executed or tested against a live Supabase project.** The Android app is not connected to Supabase yet. Do not paste service-role keys into the app or GitHub.

## Current milestone

The Android screen has seller draft preparation, local private draft saving/restoration, and buyer hand-off actions to open a seller-supplied catalogue link or WhatsApp chat. These do not publish a listing or provide public search. The latest source build has passed GitHub Actions compilation; physical-device testing remains outstanding.

## Next implementation stages

1. Create a Supabase project and apply/review the lean schema.
2. Connect Android using a client-safe publishable/anon key and strict row-level security.
3. Add seller authentication and secure business submission.
4. Add buyer search and state/city/category filters for approved businesses.
5. Add reporting, moderation and abuse controls.
6. Only after real buyer activity exists, consider optional sponsored placements and server-verified payments.

Never put a Supabase service-role key or payment secret in Android source code or GitHub. Do not charge sellers before there is real buyer traffic.

## Build and testing

A successful GitHub Actions Android build verifies compilation and artifact creation only. It does not prove that the backend is connected or that the directory works on a physical phone. Test seller submission, public browsing, moderation and outbound links before public launch.


## Buyer link hand-off (implemented in Android UI)

The current Android screen includes two direct buyer actions:
- **Open a WhatsApp catalogue link**: the buyer pastes a link intentionally shared by a seller; Android opens it with the installed app or browser.
- **Chat with a business on WhatsApp**: the buyer enters an international phone number; the app opens the wa.me chat link. For Nigeria, enter 234 followed by the national number without its leading zero.

These are link hand-off tools, not public discovery search. The public searchable directory remains pending until a shared backend exists. The app must not claim that a pasted link is verified or that a WhatsApp message was sent; the buyer reviews and sends any message in WhatsApp themselves.
