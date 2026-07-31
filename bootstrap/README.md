# Building the rootfs bootstrap

The app doesn't compile bash/curl/mpv itself — it just unpacks a zip of
pre-built binaries into private storage on first run. You need to produce
one `bootstrap-<abi>.zip` per ABI you ship (arm64-v8a, armeabi-v7a, x86_64)
and drop them into `app/src/main/assets/`.

## What has to be in the zip

A `bin/`, `lib/` (and usually `libexec/`, `etc/`) tree containing, at
minimum, binaries built against Android's bionic libc for:

- `bash`
- coreutils (`ls`, `cat`, `mkdir`, `mktemp`, etc. — ani-cli's dependency list)
- `curl`
- `grep`, `sed`, `awk`
- `fzf`
- `mpv` (+ its runtime libs — this is the biggest piece by far)
- a CA cert bundle (`etc/tls/cert.pem` or similar) so curl/mpv can hit HTTPS

## Recommended path: reuse Termux's packaging

Don't build all of this from scratch. Termux already maintains bionic-built
packages for every package above, and their bootstrap builder
(`termux-packages` repo, `scripts/build-bootstraps.sh`) is exactly the tool
that produces the `bootstrap-<abi>.zip` files Termux itself ships. Point it
at a package list containing `bash`, `curl`, `grep`, `sed`, `gawk`, `fzf`,
`mpv`, and `ca-certificates`, run it per ABI, and copy the resulting zips
here.

Repo: `termux/termux-packages` on GitHub — see `scripts/build-bootstraps.sh`
and the `docs/` folder for usage. This has to run in Termux's own build
environment (Docker image they provide); it's not something to run inside
this project.

## Licensing note

Termux's packages are a mix of upstream licenses (bash is GPLv3, curl is
MIT-style, mpv is GPLv2+/LGPL depending on config, etc.) plus Termux's own
GPLv3-licensed bootstrap tooling and terminal-view/terminal-emulator
libraries that this app depends on directly. If you distribute this app
publicly, you're on the hook for complying with all of those — GPLv3 in
particular means source availability obligations. Worth reading through
before a public release; fine for personal/sideloaded use in the meantime.

## Placing ani-cli itself

Get the actual script from the upstream repo (`pystardust/ani-cli` on
GitHub) rather than copying it from anywhere else, so you get the current
version with its current fixes. Save it as `app/src/main/assets/ani-cli`
(no extension) — `BootstrapInstaller` copies it into `$PREFIX/bin/ani-cli`
and chmods it executable on first run.
