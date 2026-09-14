# Crystal Ville Web Account Linking

CrystalCore now includes `/cvlink <6-digit-code>` for connecting a Minecraft account to the Crystal Ville Player Hub.

## Player flow
1. Create/sign in to the Crystal Ville website Player Hub.
2. Enter the exact Minecraft username and generate a 6-digit code.
3. Join the Crystal Ville Minecraft server.
4. Run `/cvlink <code>`.
5. CrystalCore verifies the code through Supabase and marks the website account as linked.

## Server configuration
`config.yml` contains the Crystal Ville Supabase project URL and **publishable** key. Publishable keys are intended for client/server API use when RLS is configured; never replace it with a Supabase service-role or secret key.

## Build
The repository already has GitHub Actions Maven build workflows. After committing these source changes, the workflow can build `CrystalCore.jar`.
