# Logo Integration Notes

## Current Implementation

`SplashScreen.kt` loads the shared UI drawable and localizes the accessibility label:

```kotlin
Image(
    painter = painterResource(id = CoreUiR.drawable.zatiaras_logo),
    contentDescription = stringResource(CoreUiR.string.brand_logo_content_description),
)
```

Keep the visible logo asset in `core/ui` when it is shared by multiple features. Keep every screen-reader label in `strings.xml`; do not inline `contentDescription = "..."` in Compose code.

## Verification Checklist

- [ ] App launches without splash crash.
- [ ] Splash screen shows the Zatiaras Juice logo with pulse animation.
- [ ] App icon appears in launcher and recent apps.
- [ ] Adaptive icon works on Android 8.0+ devices.

## Files

- `app/src/main/java/com/zatiaras/pos/navigation/SplashScreen.kt`
- `core/ui/src/main/res/drawable/zatiaras_logo.png`
- `core/ui/src/main/res/values/strings.xml`
