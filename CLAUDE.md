# Rules for Claude Code sessions in this repository

Snoozer, an Android app built from the same rules as the
`android-app` template (a private repository). The owner is **not a developer
and does not review code**. CI, tests, lint and these rules are the safety net. Follow them every
time.

## Workflow
1. **One focused change per branch and pull request.** Start a new branch from an up-to-date
   `main` (`git fetch origin main && git checkout -b <branch> origin/main`). Never reuse an old
   or merged branch for a new change.
2. **Before finishing, run and fix everything:**
   ```
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```
   If you touched `scripts/` or the workflows, also run `python3 -m unittest discover -s scripts/tests`.
   Then push and make sure CI on the pull request is green.
3. **Write the pull request description in plain language** (fill in `.github/pull_request_template.md`):
   what changed and why, in words a non-developer understands, and **exactly what to tap on the
   phone to test it** (step by step, with what they should see). The owner tests by installing
   the debug APK from the pull request's CI run (Checks → CI → Artifacts). It installs as a
   separate app, "Snoozer Dev" (`io.github.impca201.snoozer.debug`), next to the released one.
4. **Testing on the phone does not block merging.** The owner doesn't review code, so green CI is
   what a pull request needs to be merged. The owner tries the change afterwards with the test steps
   from the description (the debug APK stays on the pull request's CI run, and every push to
   `main` builds a new one) and always before a release. A problem found then is fixed in a new
   pull request.

## Never do these without asking first (and saying why)
- Delete, skip, `@Ignore`, or weaken tests, lint checks, CI steps or workflows to make something
  pass. Don't add a lint baseline or disable a lint check. **Never remove the release workflow.**
- Add Android permissions (beyond the ones explained in `PRIVACY.md`), network access (the app
  has no `INTERNET` permission), analytics, crash reporting, ads or tracking.
- Add new dependencies (libraries or Gradle plugins). Prefer what is already in
  `gradle/libs.versions.toml`. Settings are stored with **DataStore**.
- Change the `applicationId` (`appId` in `app/build.gradle.kts`) or the signing setup. The app is
  released: phones would refuse to update it.
- Rename a Tasker command id or the shortcut action: they live inside users' Tasker tasks and
  launchers.

## Never commit
Secrets, keystores (`*.jks`, `*.keystore`), `keystore.properties`, passwords or API keys.

## Code rules
- Kotlin + Jetpack Compose + Material 3, single `app` module, mainstream libraries only.
- **Every logic change gets new or updated unit tests** (`app/src/test/`). Keep logic in plain
  Kotlin classes without Android types (no `Context`, `View`, Compose) so it runs on the JVM.
  Composables only display state and forward clicks.
- **All user-visible text goes in `app/src/main/res/values/strings.xml`**, never hard-coded.
  Add the translation to **every** `values-xx/strings.xml` (currently: `nl`, `fr`, `de`, `es`, `it`).
- Dependency versions live only in `gradle/libs.versions.toml`.
- Versions are automatic (git tags / release workflow). Don't edit `versionName`/`versionCode`.
- A change to `app/src/main/res/xml/shortcuts.xml` must also be made in
  `app/src/debug/res/xml/shortcuts.xml` (the debug build's copy with the `.debug` package).

## Keep docs in sync
- Behaviour change → update `docs/` and, if it touches data or permissions, `PRIVACY.md`.
- Notable decision (new library, architecture choice, minSdk change, …) → add a dated entry to
  `docs/decisions.md`.
- Structure: `docs/structure.md`. Design and flow: `docs/architecture.md`. What is tested and
  how: `docs/testing.md`. Look and feel: `DESIGN.md`.
- `DESIGN.md`: only its "This app" section belongs to this repository. The other sections are
  copied from the master copy in the android-app template (`wimvdon/android-app`): change a
  shared rule there first, then copy it unchanged to all three apps (Tracker for Simkl, Time
  Tracker for Notion, Snoozer).

## Releasing (only when the owner asks)
Actions → Release → Run workflow on `main` → choose `patch`, `minor` or `major`.
See `docs/installation.md`, "Making a release".
