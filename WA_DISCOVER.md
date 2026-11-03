# WA Discover — marketplace foundation

WA Discover is the Nigerian business-discovery feature planned for WA Companion.

## Confirmed product decisions

- Launch audience: Nigeria-wide, with state/city filters.
- Buyers can browse without creating an account.
- Sellers must sign in before publishing or managing a business.
- Basic listings are free; paid promotion is optional.
- Buyers contact sellers through the seller's WhatsApp Business number.
- Promoted placements must be clearly labelled **Sponsored**.
- Marketplace impressions, listing opens, saves and WhatsApp taps must be measured honestly. They are not WhatsApp Status views and do not guarantee sales.

## Current milestone

The Android app now contains the first WA Discover entry point and a dedicated marketplace shell. It deliberately displays no fabricated sellers, products or performance numbers. Search and seller registration explain that the shared backend is still being connected.

The Supabase schema draft is in `supabase/schema.sql`. It includes profiles, categories, businesses, listings, promotions, listing events and reports, with initial Row Level Security policies.

## Backend setup still required

1. Create a Supabase project owned by the project maintainer.
2. Review and run `supabase/schema.sql` in the SQL Editor.
3. Verify every RLS policy using separate anonymous, seller and admin test accounts.
4. Create private image buckets and narrowly scoped Storage policies.
5. Configure Android Supabase client credentials using the publishable/anon key only.
6. Implement sign-in, seller profile creation, listing submission, approval and public browsing.
7. Add server-side payment verification before accepting any promotion payment.

Do not put a Supabase service-role key or payment secret in Android source code or GitHub. Do not make a bucket public until its contents and policies have been reviewed.

## Promotion packages (proposed, not active)

- Free basic listing
- ₦50 local boost
- ₦100 wider discovery
- ₦300 featured listing
- ₦1,000 business spotlight

These are planning prices only. No payments are currently accepted and no reach is guaranteed.

## Build

The existing GitHub Actions workflow builds the Android debug APK. A successful compile does not mean the Supabase backend is connected or the marketplace is ready for public use.
