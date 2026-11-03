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

Keep the shared directory database small. A business entry should contain only:
- Business name and short description
- Category
- State and city
- WhatsApp contact link or phone number
- WhatsApp Business catalogue link (optional)
- Optional logo/image URL only if a seller deliberately supplies one and we later support it
- Approval/status and timestamps

Do not build our own product-photo/video upload system for the first version. Do not store WhatsApp media by scraping or copying it.

## Current milestone

The Android app has a WA Discover entry point and a dedicated screen redesigned around the WhatsApp-first directory model. The screen explains the seller-to-directory-to-buyer flow and is honest that search and registration are not live yet.

The existing supabase/schema.sql is an earlier marketplace schema draft with product listings and promotions. It must be reviewed and simplified before use; it is not yet deployed or tested. The next backend should start with business directory records, categories, reports/moderation and basic directory events. Image storage is not required for the first version.

## Next implementation stages

1. Simplify the database to business profiles, categories, moderation/reports and basic directory events.
2. Connect the Android app to the shared database using a client-safe publishable/anon key and strict row-level security.
3. Implement seller submission of business details and WhatsApp catalogue/contact links.
4. Implement buyer search and state/city/category filters.
5. Add report/block and admin moderation tools.
6. Only after real buyer activity exists, consider optional sponsored placements and payment processing.

Never put a Supabase service-role key or payment secret in Android source code or GitHub. Payment verification, if added later, must be server-side. Do not charge sellers before there is real buyer traffic.

## Build and testing

A successful GitHub Actions Android build verifies compilation and artifact creation only. It does not prove that the backend is connected or that the directory works on a physical phone. Test seller submission, public browsing, moderation and outbound links before public launch.
