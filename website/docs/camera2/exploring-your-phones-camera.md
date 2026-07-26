---
sidebar_position: 4
title: "Chapter 4: Exploring Your Phone's Camera"
description: Use the Android Camera Parameters app to explore your device's camera capabilities, including camera IDs, resolutions, FPS, hardware level, RAW support, and more.
keywords: [camera parameters, camera IDs, hardware level, RAW support, FPS, resolution]
---

This is where the learning becomes interactive. Let's explore your phone's camera!

## Introduction

Reading about cameras is helpful, but nothing beats seeing real data from your own device. That's where Android Camera Parameters comes in.

This chapter is all about **exploration**. No coding yet. Just curiosity.

## Install Android Camera Parameters

If you haven't already, install the Android Camera Parameters app on your Android device:

1. Open Google Play Store
2. Search for "Android Camera Parameters"
3. Install the app
4. Open it and grant camera permissions

## Understanding the Interface

When you open the app, you'll see several sections:

### Overview Screen
Shows basic camera information at a glance:
- Camera IDs
- Lens facing
- Hardware level
- Sensor size
- Available capabilities

### Categories Screen
Organizes camera parameters into logical groups:
- Camera info
- Sensor
- Lens
- Control
- Scaler
- Flash
- And more

### RAW JSON Screen
Displays the complete CameraCharacteristics in raw JSON format for advanced users.

## Let's Explore

Let's go through the key pieces of information you should look for.

### Camera IDs

Android assigns each camera a unique ID number. Look for:
- **Camera 0** — Usually the rear wide camera
- **Camera 1** — Could be the front camera or another rear camera
- **Camera 2** — Often the ultra-wide or telephoto camera
- **Camera 3+** — Additional cameras (macro, depth, etc.)

Each ID represents a separate camera device with its own characteristics.

### Hardware Level

This is one of the most important pieces of information:

| Level | Description |
| --- | --- |
| **LEGACY** | Old devices, limited Camera2 support |
| **LIMITED** | Basic Camera2 features |
| **FULL** | Full manual controls, RAW support |
| **LEVEL_3** | Advanced features like YUV reprocessing |

Check what hardware level your phone supports. This determines what Camera2 features are available.

### Sensor Information

Look for:
- **Sensor size** — Physical dimensions of the sensor
- **Active array size** — The actual area used for capturing images
- **Pixel array size** — Total pixels on the sensor
- **Max frame duration** — Minimum time between frames
- **Output formats** — JPEG, RAW, YUV, etc.

### Resolution Options

Cameras support multiple resolutions. Check:
- **Preview sizes** — Resolutions available for display
- **Picture sizes** — Resolutions available for photo capture
- **Video sizes** — Resolutions available for video recording

Notice the different aspect ratios: 4:3, 16:9, 1:1.

### Frame Rate (FPS)

Look for:
- **Preview FPS range** — Frames per second for the preview
- **Capture FPS range** — Frames per second for still capture
- **High-speed video** — Special high frame rate modes

Higher FPS means smoother video and more responsive autofocus.

### RAW Support

Check if your camera supports RAW capture:
- **RAW formats** — RAW_SENSOR, RAW10, RAW12, RAW16
- **RAW sizes** — Available resolutions for RAW capture

RAW support requires at least FULL hardware level.

### Flash Capabilities

Look for:
- **Flash mode** — OFF, ON, AUTO, TORCH
- **Available modes** — What flash features are supported
- **Flash info** — Flash strength and capabilities

### Zoom

Check:
- **Max digital zoom** — How much you can digitally zoom
- **Available focal lengths** — Different lenses and their focal lengths
- **Smooth zoom support** — Whether zoom can be adjusted smoothly

### Focus Modes

Look for:
- **Available focus modes** — AUTO, FIXED, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDGE
- **Focus distance range** — Minimum and maximum focus distance
- **AF regions** — Number of autofocus regions supported

### Exposure Control

Check:
- **AE modes** — AUTO, ON, OFF
- **Available AE modes** — What exposure modes are supported
- **Exposure range** — Minimum and maximum exposure times
- **ISO range** — Supported ISO values

## Your Turn

Now it's your turn to explore. Answer these questions about your phone:

1. How many cameras does your phone have?
2. What hardware level do they support?
3. Which camera supports RAW?
4. What's the highest resolution available?
5. Does any camera support 4K video?
6. What's the maximum zoom level?
7. Does your phone have a telephoto or ultra-wide camera?

## Why This Matters

You might wonder why we're exploring before coding. Here's why:

1. **Every phone is different** — What works on one device may not work on another
2. **Camera2 requires adaptation** — Good Camera2 apps query capabilities, not assume them
3. **Understanding builds intuition** — When you see real data, abstract concepts become concrete

When we start coding, you'll already know what to expect from your device.

## Compare with Friends

If you have friends with different phones, compare your findings:
- Does the flagship phone have better hardware level?
- Do budget phones lack RAW support?
- How do camera counts vary?

This helps you understand the Android camera ecosystem.

## Common Discoveries

Here are some common things people discover:

- **Flagship phones** often have FULL or LEVEL_3 hardware level
- **Budget phones** often have LIMITED or LEGACY hardware level
- **Most phones** support JPEG capture
- **RAW support** is still not universal
- **Multiple cameras** are standard on modern phones
- **Front cameras** usually have lower resolution than rear cameras

## Next Chapter

Now that you've explored your phone's camera, you're ready to start coding! In the next chapter, we'll introduce the first Camera2 class: **CameraManager**.

CameraManager is the entry point to the Camera2 API. It allows you to:
- Enumerate available cameras
- Get camera characteristics
- Open cameras

Let's begin!

## Summary

Exploring your phone's camera is the best way to understand what Camera2 can do. Android Camera Parameters makes this easy by displaying all camera capabilities in an organized way.

Key things to look for:
- Camera IDs and their roles
- Hardware level (LEGACY, LIMITED, FULL, LEVEL_3)
- Resolution options
- RAW support
- Flash and zoom capabilities
- Focus and exposure controls

This hands-on exploration builds the foundation for writing Camera2 applications.