-- Public objects remain readable by URL; do not expose object listing.
drop policy if exists "Public can view Meme War submission images" on storage.objects;
