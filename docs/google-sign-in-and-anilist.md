# Google sign-in and AniList metadata

## Google account

Rei Stream uses Android Credential Manager and Google Identity Services for an optional, device-side Google sign-in. Before enabling it in a distributable build, create an Android OAuth client and a **Web application client** in Google Cloud, register the package/signing certificate, then provide the Web client ID outside source control:

```properties
# local.properties (never commit this value)
google.web_client_id=YOUR_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
```

CI can instead set `GOOGLE_WEB_CLIENT_ID`. The app retains only the selected account's email/name; it does not retain the Google ID token and does not upload folder names, file names, or the local library to Google. A future cloud-sync backend must verify each token server-side before using it.

## AniList

The local-library refresh calls AniList's public GraphQL endpoint with a single parser-derived anime title. It does not send paths or video files. A successful lookup stores synopsis, alternate names, genres, year, status, episode count and image URLs locally. Poster bytes are cached under the app cache directory; subsequent refreshes use the metadata cache unless the caller requests a forced metadata refresh.

The refresh control is intentionally manual. Opening the app or library does not re-scan storage and does not make AniList requests.
