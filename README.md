<h1 align="center">Pin Lock</h1>

<p align="center">
  <a href="http://developer.android.com/index.html"><img alt="Android" src="https://img.shields.io/badge/platform-android-green.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
</p>

<p align="center">
🔐 Light library that is beautiful Pin Lock screen for Jetpack Compose. The library handles saving pin in Encrypted file. Integration is very easy and fast.
</p>

# Download

```kotlin
repositories {
  mavenCentral()
}

dependencies {
  implementation("com.zaneschepke:pin_lock_compose:1.0.5")
}
```

# Implementation

Firstly, initialize the library on your `onCreate` of Application class:
```kotlin
PinManager.initialize(this)
```

---

To add Pin Lock screen, add the `PinLock` composable to your compose area:
```kotlin
PinLock(
  title = { pinExists ->
    Text(text = if (pinExists) "Enter your pin" else "Create pin")
  },
  color = MaterialTheme.colorScheme.primary,
  onPinCorrect = {
    // pin is correct, navigate or hide pin lock
  },
  onPinIncorrect = {
     // pin is incorrect, show error
  },
  onPinCreated = {
     // pin created for the first time, navigate or hide pin lock
  }
)
```

If there is no saved pin yet, it promtps the user to create pin. If there is saved pin, it promts the user to enter its pin:

<p align="center">
  <img width="296" height="600" src="https://github.com/zaneschepke/pin_lock_compose/blob/master/banner_1.gif" />
  <img width="296" height="600" src="https://github.com/zaneschepke/pin_lock_compose/blob/master/banner_2.gif" />
</p>

---

It is also possible to change pin. Just add `ChangePinLock` composable to your compose area:
```kotlin
ChangePinLock(
  title = { authenticated ->
    Text(text = if (authenticated) "Enter new pin" else "Enter your pin")
  },
  color = MaterialTheme.colorScheme.primary,
  onPinIncorrect = {
    // pin is incorrect, show error
  },
  onPinChanged = {
    // pin changed, navigate or hide pin lock
  }
)
```
Use this only if there is already saved pin. If there is no saved pin, use simple `PinLock` instead for creating pin for the first time. When using `ChangePinLock`, firstly it prompts the user to enter original pin. After user succesfully authenticates using his original pin, it prompts the user to creat a new pin:

<p align="center">
  <img width="296" height="600" src="https://github.com/zaneschepke/pin_lock_compose/blob/master/banner_3.gif" />
</p>

---

To check if saved pin exists:
```kotlin
val pinExists = PinManager.pinExists()
```

To clear saved pin so user can create brand new pin:
```kotlin
PinManager.clearPin()
```

# Features

- pin lock screen very easily
- handles encryption and saving pin internally
- change pin lock screen
- customizable background color
- robust to configuration changes
- backspace to remove last entered pin number
- incorrect indicator animation

# Demo

You can install and try demo app. All the features are implemented in the demo from creating pin to changing pin.

<a href="https://github.com/zaneschepke/pin_lock_compose/blob/master/app-debug.apk">Download demo</a>

<p align="center">
  <img width="296" height="600" src="https://github.com/zaneschepke/pin_lock_compose/blob/master/screenshot_1.jpg" />
  <img width="296" height="600" src="https://github.com/zaneschepke/pin_lock_compose/blob/master/screenshot_2.jpg" />
</p>
