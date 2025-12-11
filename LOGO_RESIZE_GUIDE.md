# Logo Resize Guide

Quick reference for adjusting logo sizes in your app.

## 📱 App Icon (Launcher Icon)

**File:** `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Lines to change:** Lines 8-9
```xml
<item
    android:width="72dp"    ← Change this
    android:height="72dp"   ← Change this
    android:gravity="center">
```

### Recommended Sizes:
- **Small:** 60dp × 60dp
- **Default:** 72dp × 72dp ✅ (current)
- **Large:** 84dp × 84dp
- **Extra Large:** 90dp × 90dp

**Note:** Don't go above 90dp as it may get clipped on some devices.

---

## 🔐 Splash Screen Logo (Authentication Screens)

**File:** `app/src/main/java/com/masterpushers/pass_assist/ui/AuthScreen.kt`

**Lines to change:** Lines 148, 262, and 362
```kotlin
.size(100.dp)  ← Change this value
```

### Recommended Sizes:
- **Small:** 80dp
- **Default:** 100dp ✅ (current)
- **Large:** 120dp
- **Extra Large:** 140dp

**Note:** Appears on:
- Biometric authentication screen
- PIN entry screen
- PIN setup screen

---

## 🎯 Quick Change Instructions

### To make logos LARGER:
1. **App Icon:** Change 72dp to 84dp or 90dp
2. **Splash Screen:** Change 100.dp to 120.dp or 140.dp

### To make logos SMALLER:
1. **App Icon:** Change 72dp to 60dp
2. **Splash Screen:** Change 100.dp to 80.dp

### To match sizes across both:
- **Proportional ratio:** App icon should be ~70-75% of splash screen logo
- Example: App icon 72dp → Splash screen 100dp (72%)

---

## 🔄 After Changing

1. **Rebuild the app:**
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

2. **Clear app data (if icon doesn't update):**
   ```bash
   adb shell pm clear com.masterpushers.pass_assist
   ```

3. **Force icon refresh (alternative):**
   - Uninstall the app
   - Reinstall the app

---

## 💡 Tips

- **App Icon:** Smaller is often better for clean, modern look
- **Splash Screen:** Larger is more prominent but don't overwhelm the screen
- **Padding:** The 8.dp padding on splash screen provides breathing room
- **Test on device:** Sizes may look different on various screen sizes

---

## 📐 Current Settings

| Location | Size | Status |
|----------|------|--------|
| App Icon | 72dp × 72dp | Default ✅ |
| Splash Screen | 100dp (92dp effective with padding) | Default ✅ |

---

**Last Updated:** After implementing logo across all screens

