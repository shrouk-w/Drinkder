# Drinkder is a mock app of Tinder, allows users to swipe between drinks (including non-alcoholic).

##press fav tab a few times for easter egg

## API configuration

Copy `.env.example` to `.env` in the project root and set `API_KEY` to your
TheCocktailDB production key (digits only, without quotes). Rebuild the app
after changing the key. All TheCocktailDB API requests use this key through
the shared Retrofit client; there is no fallback to the public test key.
With an empty key the project builds, but API requests fail with a configuration error.

`.env` is ignored by Git. Gradle embeds the key in `BuildConfig`, so it is
included in the APK and can be extracted; `.env` prevents committing it,
but does not make a key distributed in an Android app secret.
