# Free My TV — Xiaomi integration workstream

Target TVs: MiTV-MZTU1 and MiTV-M00Q1. Keep existing launcher behavior and settings.

## Verified source audit (uploaded FreeMyTV-android-build-ready.zip)
- Android app sources: app/src/io/freemytv/.
- RemoteKeys.java already handles Xiaomi MiTV-M models and delegates button mappings to Prefs.keyApp().
- Prefs.java already persists remapping choices to a keys file; KEY_XIAOMI_OKKO and KEY_XIAOMI_IVI are included in KEYS.
- AndroidManifest.xml has INTERNET permission but no VpnService declaration.
- No embedded VLESS/Reality engine has been verified.
- Current GitHub main branch has no accessible source files; source import is required before changes can be built.

## Required behavior
- Dark theme; favorites; internet speed indicator.
- Built-in VLESS/Reality VPN with subscription import, server list, latency tests, on-demand speed tests, server selection and reconnect.
- Never auto-connect at boot; persist last server, connect only by user action.
- Configurable remote mapping, default IVI -> VPN toggle and Okko -> Free My TV; others retain normal behavior. Preserve ability to override all mappings.
- Android VpnService user consent is required. A compatible tun-capable core must be bundled; a settings shortcut alone is not a VPN.
- Xiaomi system may intercept some global shortcut keys; test both devices.
- Never commit subscription credentials, private signing keys, or user-specific config.

## Delivery gates
1. Import source tree safely into feature branch, excluding keystores, build outputs and secrets.
2. Implement and review features in source.
3. Build signed development APK and inspect manifest.
4. Validate on both TVs (remote behavior and VPN routing require device testing).

Status: architecture/source audit only; no VPN functionality or final APK is claimed.
