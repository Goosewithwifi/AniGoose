# AniGoose

A single-purpose Android app: it's a terminal emulator that boots straight
into `ani-cli` and only ani-cli. No launcher for other commands, no way to
drop to a bare shell — when ani-cli exits, it's relaunched automatically.

Package: `com.anigoose.app` · Gradle project name: `AniGoose`

## How it works

1. **UI**: Termux's `TerminalView` / `TerminalSession` (GPLv3) render a real
   pty-backed terminal — no custom VT100 parser needed.
2. **Rootfs**: a minimal bionic-linked userland (bash, coreutils, curl, grep,
   sed, fzf, mpv) is bundled as a zip per ABI in `assets/`, unpacked into the
   app's private storage (`filesDir/usr`) on first launch by
   `BootstrapInstaller`.
3. **Lockdown**: `TerminalActivity` starts exactly one session:
   `bash -lc 'exec ani-cli'`. The `exec` means there's never an intermediate
   shell prompt for the user to type other commands into. On exit, the
   session is restarted automatically rather than falling through to a
   prompt. Back button backgrounds the app instead of exposing recents/launcher
   mid-episode.

## Building on GitHub

`.github/workflows/build.yml` builds a debug APK on every push/PR via
GitHub Actions, and uploads it as a workflow artifact you can download from
the Actions tab. It uses the checked-in Gradle wrapper (`./gradlew`), so no
local Gradle install is required — just push this repo to GitHub and the
workflow runs automatically.

The workflow automatically downloads the current `ani-cli` script from
`pystardust/ani-cli` (master branch) into `assets/` before each build, so
you always get the latest upstream version without committing it yourself.
If you'd rather pin a specific version, commit your own copy to
`app/src/main/assets/ani-cli` — the workflow detects it and leaves it alone
instead of overwriting it.

The workflow will still succeed (compile + package an APK) even without
the `bootstrap-<abi>.zip` rootfs assets described below — those are the one
piece it can't fetch automatically — but the resulting APK won't be able to
actually run ani-cli until those are added. The workflow prints a warning
in that case rather than failing, so you can tell CI is green for the
right reason.

**Licensing note:** ani-cli is GPLv3-licensed. Auto-bundling it into the
APK on every build means every build is subject to GPLv3's source
availability requirements if you distribute the APK — see
`bootstrap/README.md` for the fuller licensing picture (Termux's tooling
and terminal libraries add their own obligations on top of this).

## App icon

Launcher icon is wired up as an Android **adaptive icon** (background color
+ foreground art), which is the modern standard and displays correctly on
every device/launcher shape (circle, squircle, rounded square).

The current icon is your goose-with-a-router logo, cut out and centered
with safe padding so it isn't clipped by circular/squircle launcher masks.
It's a raster PNG (not a vector), supplied at every density bucket:
- `app/src/main/res/mipmap-{m,h,xh,xxh,xxx}hdpi/ic_launcher_foreground.png`
  — the adaptive-icon foreground layer, used on Android 8+
- `app/src/main/res/mipmap-{m,h,xh,xxh,xxx}hdpi/ic_launcher.png` +
  `ic_launcher_round.png` — flattened fallback for pre-Android-8 devices
  that don't support adaptive icons at all
- `app/src/main/res/values/ic_launcher_background.xml` — the dark
  background color (`#0D0D0D`) behind the goose

**To swap in a different logo later**, easiest is Android Studio's Image
Asset tool: right-click `app/src/main/res` → New → Image Asset → "Launcher
Icons (Adaptive and Legacy)" → upload your art as the foreground layer. It
regenerates every file above automatically. Manually, just replace those
same files at the same sizes.

## What's here vs. what you still need to build

Written and ready:
- Gradle project (with wrapper), CI workflow, manifest, `TerminalActivity`,
  `BootstrapInstaller`, layout/strings, launcher icon

Not included (see `bootstrap/README.md`):
- The actual `bootstrap-<abi>.zip` files — these are large pre-built binary
  archives (mpv alone pulls in ffmpeg), built via Termux's own packaging
  scripts, not something to generate inside a chat session
- The `ani-cli` script itself — grab current upstream from `pystardust/ani-cli`

## Open design questions worth deciding before you build for real

- **mpv rendering**: mpv wants to draw to a SurfaceView, not stdout — ani-cli
  normally shells out to a system mpv binary and lets it own the window. In a
  locked-down single-activity app you'll want to either (a) let mpv fork as a
  subprocess with its own window (simplest, matches how ani-cli already
  works), or (b) embed an `mpv-android`-style SurfaceView for a single unified
  window. (a) is far less work and is what this scaffold assumes.
- **Storage permissions**: ani-cli/mpv don't need broad storage access unless
  you want downloaded episodes to persist outside app-private storage.
- **fzf inside a touch-screen terminal**: works, but arrow-key/search
  navigation is awkward without a hardware keyboard — worth testing early
  since it's core to ani-cli's UX (show picker, episode picker).
