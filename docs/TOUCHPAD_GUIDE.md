# 🖱️ Touchpad Mouse User & Gesture Guide

**Type4Me** includes a built-in, low-latency precision **Touchpad Mouse** that transforms your Android phone into an ergonomic wireless trackpad for your PC, Mac, or Linux workstation over the existing Bluetooth connection.

---

## 🌟 Features Overview

- 🎯 **Sub-Millisecond Response**: Transmits raw 4-byte HID mouse reports directly to the host's OS kernel with zero perceptible lag.
- 🖐️ **Intuitive Multi-Touch Gestures**: Seamless glide, tap-to-click, and long-press right click.
- 📜 **Thumb-Accessible Scroll Strip**: Dedicated vertical scroll strip along the right edge of the screen for ultra-smooth webpage and code navigation.
- 🖲️ **Physical Click Bars**: High-visibility bottom buttons for Left Click, Middle Click (scroll wheel press), and Right Click.
- ⚡ **Adjustable Sensitivity Curve**: Speed multiplier slider ranging from $0.5\times$ to $3.0\times$.

---

## 🕹️ Gestures Reference

| Gesture | Action on Host PC | Description |
| :--- | :--- | :--- |
| **1-Finger Drag** | **Move Pointer** | Glides the mouse cursor smoothly across the host workstation display. |
| **1-Finger Tap** | **Left Click** | Standard primary click to select files, click buttons, or focus windows. |
| **1-Finger Double-Tap** | **Double Click** | Opens files, folders, or selects words in text editors. |
| **Long Press** | **Right Click** | Opens context menus or inspector panels. |
| **2-Finger Tap** | **Right Click** | Alternative two-finger tap to trigger context menus. |
| **Drag on Right Strip** | **Vertical Scroll** | Smooth upward or downward scrolling (drag up to scroll down, drag down to scroll up). |

---

## 🖲️ Hardware & Screen Controls

```
+-----------------------------------------------------------+
| [🎙️ Voice Keyboard]            [🖱️ Touchpad Mouse (Active)]|
+-----------------------------------------------------------+
|  Touchpad Trackpad                             Speed: 1.2x |
| +-----------------------------------------+ +-----------+ |
| |                                         | |     ▲     | |
| |                                         | |     |     | |
| |          PRIMARY TRACKPAD AREA          | |  SCROLL   | |
| |         (1-Finger Glide & Tap)          | |   STRIP   | |
| |                                         | |     |     | |
| |                                         | |     ▼     | |
| +-----------------------------------------+ +-----------+ |
| Speed: [====●=================] 1.2x                      |
| +---------------------+ +---------+ +-------------------+ |
| |     LEFT CLICK      | |   MID   | |    RIGHT CLICK    | |
| |       (55%)         | |  (18%)  | |       (35%)       | |
| +---------------------+ +---------+ +-------------------+ |
+-----------------------------------------------------------+
```

### 1. Primary Trackpad Area
* Drag anywhere on the dark carbon canvas to move the host mouse pointer.
* Sensitivity scaling applies dynamically in real-time.

### 2. Vertical Scroll Strip
* Positioned on the right side of the screen for natural one-handed thumb scrolling.
* Relies on native OS smooth-scrolling physics.

### 3. Dedicated Tactile Click Buttons
* **Left Click Button (55% width)**: Primary action button.
* **Middle Click Button (18% width)**: Triggers middle-click actions (e.g. open links in new browser tabs, paste in Linux terminals, auto-scroll).
* **Right Click Button (35% width)**: Secondary context menu button.

---

## ⚙️ Sensitivity & Speed Tuning

Use the **Speed** slider below the trackpad canvas to adjust pointer velocity:
* **0.5x – 0.8x**: High precision (ideal for graphic design, CAD, or tiny UI elements).
* **1.0x – 1.4x (Recommended)**: Balanced velocity for general desktop productivity and coding.
* **1.5x – 3.0x**: High-speed traversal across dual/triple multi-monitor workstation setups.

---

## 🔌 Technical Report Format (Report ID 2)

Type4Me formats mouse movement into 4-byte standard USB HID mouse packets:

```kotlin
// Byte 0: Buttons bitmask (Bit 0: Left, Bit 1: Right, Bit 2: Middle)
// Byte 1: Signed 8-bit relative X movement (-127 to +127)
// Byte 2: Signed 8-bit relative Y movement (-127 to +127)
// Byte 3: Signed 8-bit relative vertical scroll (-127 to +127)
val mousePacket = byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), wheel.toByte())
```
