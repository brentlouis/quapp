# Google sign-in presentation

The action remains disabled. Quapp has no connected Google authentication service.

Brand guidance checked on 2026-10-21:
https://developers.google.com/identity/branding-guidelines

Official artwork archive:
https://developers.google.com/static/identity/images/signin-assets.zip

`google_g_official.png` is the central 80 × 80 logo region (40,40 to 120,120) of
`Android + Web/PNG @4x/Light/Theme=Light, Show text=No, Shape=Square, Platform=Android+Web@4x.png`.
Pixels and colors are unchanged; only the surrounding icon-button boundary is excluded.
Android displays the logo at 20dp, without tint, on white with 10dp between logo and label.
The control retains Android font scaling and a minimum 48dp target.

Google Sans Medium is the official static weight-500 TrueType response to
https://fonts.googleapis.com/css?family=Google+Sans:500
using User-Agent `Mozilla/5.0`. The resolved source URL is saved in the build evidence's
`google-sans.css`. It applies only to this branded action; Quapp's native type remains unchanged.
The SIL Open Font License and copyright notice ship in
`app/src/main/assets/licenses/google_sans_ofl.txt`.

Genuine integration requires Android and Web OAuth clients, package/signing registration,
stable compatible Credential Manager/Google ID libraries, INTERNET permission, a real HTTPS
token-verification endpoint and a server-issued app-session/sign-out contract. None is
configured here. No placeholder endpoint, token acceptance, Gmail scope, or fake Google session
was added. Documentation examples containing alpha libraries were not copied.
