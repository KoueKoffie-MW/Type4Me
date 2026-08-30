# 📱 Hardware Compatibility & Bluetooth HID Specification

**Type4Me** turns your Android smartphone into a standard hardware Bluetooth peripheral using the official **Bluetooth Human Interface Device (HID) Profile** (`BluetoothProfile.HID_DEVICE`).

This document provides technical background on hardware requirements, supported chipsets, OEM stack implementations, and diagnostic troubleshooting steps.

---

## 🛠️ Android Bluetooth HID Architecture

Unlike software-based remote keyboards that require companion PC servers or browser extensions, Type4Me communicates directly with the host operating system's native Bluetooth stack.

```
+-------------------------------------------------------------+
|                      ANDROID SMARTPHONE                     |
|                                                             |
|   +-----------------------------------------------------+   |
|   |             Type4Me Application Layer               |   |
|   |         (TouchpadCanvas / KeystrokeDispatcher)      |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|   +--------------------------v--------------------------+   |
|   |          android.bluetooth.BluetoothHidDevice       |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|   +--------------------------v--------------------------+   |
|   |         Fluoride / Gabeldorsche Bluetooth Stack     |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|   +--------------------------v--------------------------+   |
|   |           Hardware Abstraction Layer (HAL)          |   |
|   |        android.hardware.bluetooth@1.x / HID-HAL     |   |
|   +--------------------------+--------------------------+   |
|                              |                              |
|   +--------------------------v--------------------------+   |
|   |             Bluetooth SoC Controller / BLE          |   |
|   +-----------------------------------------------------+   |
+------------------------------+------------------------------+
                               | Standard 8-Byte Keystrokes
                               | Standard 4-Byte Mouse Reports
+------------------------------v------------------------------+
|                   HOST PC / WORKSTATION                     |
|    (Windows 10/11, macOS, Linux Kernel, ChromeOS, iPadOS)   |
|                                                             |
|   +-----------------------------------------------------+   |
|   |        Generic USB/Bluetooth HID In-Box Driver      |   |
|   +-----------------------------------------------------+   |
|                              |                              |
|   +--------------------------v--------------------------+   |
|   |   IDE / Terminal / ChatGPT / Claude / Antigravity   |   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
```

---

## 📋 Comprehensive Compatibility List

### 1. Google Pixel Series
* **Google Pixel 10, 10 Pro (Reference Verified Device)**
* Google Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold
* Google Pixel 8, 8 Pro, 8a
* Google Pixel 7, 7 Pro, 7a
* Google Pixel 6, 6 Pro, 6a
* Google Pixel 5, 5a, 4, 4 XL, 4a, 3, 3 XL, 2
* *Status*: **100% Native Support** (Google Fluoride Bluetooth stack with complete HID peripheral role).

### 2. Samsung Galaxy Series
* **Galaxy S Series**: S25, S25+, S25 Ultra, S24, S23, S22, S21, S20, S10, S9
* **Galaxy Z Series**: Galaxy Z Fold 1 through 6, Galaxy Z Flip 1 through 6
* **Galaxy Note Series**: Note 20, Note 10, Note 9
* **Galaxy A Series**: A55, A54, A53, A52, A73, A72, A35, A34
* **Galaxy Tab Series**: Galaxy Tab S10, S9, S8, S7
* *Status*: **Full Support** on One UI 2.0+ (Android 10+).

### 3. OnePlus / OPPO / Realme
* **OnePlus**: OnePlus 13, 12, 11, 10 Pro, 9 Pro, 8 Pro, 7 Pro, 6T, Nord 2/3/4
* **OPPO**: Find X2 through X8, Reno series
* **Realme**: GT and Pro series
* *Status*: **Full Support** on OxygenOS / ColorOS 11+.

### 4. Sony Xperia
* Xperia 1 (Mark I through VI)
* Xperia 5 (Mark I through V)
* Xperia 10 (Mark I through VI)
* Xperia PRO, PRO-I
* *Status*: **Full Support** (Clean AOSP-based Bluetooth implementation).

### 5. Motorola
* Motorola Edge series (30, 40, 50 Pro/Ultra)
* Motorola Razr series (2022, 40 Ultra, 50 Ultra)
* Moto G Stylus 5G, Moto G Power 5G
* *Status*: **Full Support** on Android 12+.

### 6. Nothing & ASUS
* **Nothing**: Phone (1), Phone (2), Phone (2a)
* **ASUS**: ROG Phone 3 through 8 Pro, Zenfone 8 through 11 Ultra
* *Status*: **Full Support**.

### 7. Xiaomi / POCO / Redmi
* Xiaomi 15, 14, 13, 12, 11 series
* POCO F6, F5, F4, X6 Pro, X5 Pro
* Redmi Note 12, 13, 14 Pro/Pro+
* *Status*: **Supported** on standard global HyperOS and MIUI builds.

---

## 🚫 Unsupported Hardware Configurations

* **Android Go Edition Devices**: Stripped-down Android builds frequently omit the Bluetooth HID HAL to reduce ROM footprint.
* **Low-End Third-Party TV Sticks / Budget Chipsets**: Certain budget SoC vendors disable peripheral HID roles in their proprietary kernel drivers.

---

## 🔍 Diagnostic Verification via ADB

To verify whether your Android device supports Bluetooth HID Device mode:

```bash
# Check if Bluetooth HID Device Profile proxy service is present
adb shell dumpsys bluetooth_manager | grep -i "hid"
```

If your device supports HID peripheral mode, you will see `BluetoothHidDevice` active or bound in the service dump.
