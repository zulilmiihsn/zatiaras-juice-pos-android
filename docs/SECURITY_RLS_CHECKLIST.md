# Supabase RLS Production Checklist

The Android client uses `SUPABASE_ANON_KEY`. This key is public by design, so all data protection must be enforced by Supabase Auth and Row Level Security.

## Required Before Production

- RLS is enabled on every table accessed by the Android client.
- No Android build contains a Supabase service-role key.
- Policies restrict reads and writes by authenticated user, branch, and role where applicable.
- Write policies prevent clients from setting trusted server fields such as audit timestamps, role escalation fields, and ownership fields.
- Storage buckets used by product images have explicit read/write policies.
- Edge Functions that call AI providers validate Supabase bearer tokens before invoking third-party APIs.
- Supabase dashboard logs are reviewed after a full app smoke test for denied access, unexpected anonymous access, and broad table scans.

## Verification Scenarios

- Unauthenticated user cannot read or write POS data.
- Cashier cannot update owner-only settings, locked routes, or owner PIN data.
- User from one branch cannot read or write another branch's products, transactions, reports, or settings.
- Deleted or inactive users cannot authenticate or sync data.
- Android release APK contains only the anon key and public DSN-style config values.
