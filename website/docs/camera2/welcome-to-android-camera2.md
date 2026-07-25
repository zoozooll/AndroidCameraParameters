---
sidebar_position: 1
title: "Chapter 1: Welcome to Android Camera2"
description: Learn why Android Camera2 matters, how it differs from CameraX, and what this series will cover.
keywords: [Android Camera2, CameraX, camera capabilities, Android camera development]
---

Before controlling the camera, we need to understand the camera system.

## Introduction

Almost every smartphone today has a powerful camera system. A modern phone can:

- Capture professional-looking photos
- Record 4K and 8K videos
- Create portrait effects
- Shoot in extreme low light
- Capture slow-motion videos
- Generate depth information
- Combine multiple cameras together

But when you open the default camera app, you only see a simple interface: a shutter button, a zoom control, and a few shooting modes.

Behind this simple interface is a surprisingly complex system. The camera app communicates with hardware components, image processors, and Android frameworks to produce every frame.

As Android developers, we may want to build applications that go beyond the default camera app, such as:

- A manual photography application
- A camera testing tool
- A computer vision application
- A 3D scanning application
- A professional video recorder
- A camera capability analyzer

To build these applications, we need to understand the Android Camera2 API.

## What is Android Camera2?

Android Camera2 is the modern camera framework introduced by Google in Android 5.0 (API level 21). It replaced the original Android Camera API.

The old Camera API was designed for a simpler world: one camera, basic photo capture, and simple video recording. Smartphone cameras have evolved dramatically since then. Modern devices may contain multiple rear cameras, wide-angle lenses, telephoto lenses, depth sensors, and external cameras.

They also support advanced features:

- Manual exposure and focus
- RAW image capture
- High-speed video
- HDR processing
- Optical stabilization

Camera2 was created to give developers much deeper control over camera hardware.

## Camera2 vs. CameraX

Camera2 and CameraX solve different problems.

### CameraX

CameraX is a higher-level library designed to make common camera tasks easier, including:

- Displaying a preview
- Taking photos
- Recording videos
- Handling device compatibility

Most applications should start with CameraX.

### Camera2

Camera2 is the lower-level framework. It gives developers direct access to camera capabilities, including sensor information, exposure settings, focus controls, camera metadata, hardware capabilities, and RAW support.

Camera2 is more complex, but it provides much more control.

| | CameraX | Camera2 |
| --- | --- | --- |
| Level | High-level library | Low-level API |
| Difficulty | Easier | More complex |
| Control | Limited | Extensive |
| Best for | Normal camera apps | Advanced camera applications |

This tutorial series focuses on Camera2 because understanding it helps us understand how Android cameras actually work.

## Why learn Camera2?

You may ask: *Why should I learn Camera2 when CameraX already exists?*

### Understand what the device can really do

Every Android phone is different. One device may support RAW capture, 4K 60 fps video, and manual controls; another may not. Camera2 allows applications to discover these capabilities.

### Build professional camera applications

Applications that need advanced camera features usually need Camera2. Examples include professional camera apps, scientific imaging apps, AR applications, computer vision systems, and video production tools.

### Understand smartphone photography

Many modern camera features are based on concepts exposed through Camera2:

- Exposure
- ISO
- Focus
- White balance
- HDR
- Multiple cameras

Learning Camera2 also teaches you how smartphone cameras work.

## What will you learn in this series?

This series is designed to take you from beginner to advanced.

### Part 1: Understanding cameras

You will learn how smartphone cameras work, what camera hardware contains, how Android represents cameras, and how to inspect your own device.

### Part 2: Your first Camera2 application

You will learn how to find and open cameras, create a preview, and capture images.

### Part 3: Camera controls

You will learn about exposure, ISO, focus, white balance, flash, and zoom.

### Part 4: Advanced camera features

You will learn about RAW capture, high-speed video, multi-camera devices, logical and physical cameras, camera extensions, and HDR features.

### Part 5: Camera metadata deep dive

You will explore important Camera2 parameters, including:

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

You will understand not only what these parameters mean, but also why they exist.

## Learning with Android Camera Parameters

Reading documentation is useful, but camera capabilities are easier to understand when you can see real data from a real phone. Throughout this series, we will use [Android Camera Parameters](/) to explore actual camera information.

You can use the app to discover:

- Available cameras
- Supported resolutions
- Frame rates
- Sensor information
- Manual control support
- RAW capability
- Hardware level

Instead of learning from abstract examples, you can directly investigate your own device.

## Who is this tutorial for?

This series is designed for:

- **Android developers** who want to understand the camera system beyond basic APIs.
- **Camera app developers** who need advanced camera features.
- **Computer vision developers** who need access to camera frames and metadata.
- **Curious developers** who want to understand how smartphone cameras actually work.

## Before we start coding

Camera2 is not difficult because the API is poorly designed. It is difficult because modern cameras are extremely powerful.

A smartphone camera is no longer just a sensor that captures images. It is a complete imaging system involving:

- Hardware
- Firmware
- ISP processing
- Android framework
- Application software

Camera2 exposes this complexity to developers. Our goal in this series is to understand it step by step.

## Next chapter

In the next chapter, **Understanding Smartphone Camera Hardware**, we will leave Android for a moment and explore the camera itself. You will learn what a camera sensor does, why larger sensors produce better images, what lenses actually mean, how ISP processing works, and why two phones with similar megapixel counts can produce completely different photos.

Once you understand the hardware, Camera2 concepts will become much easier.

## Summary

Android Camera2 is the foundation for building advanced camera applications on Android. It provides direct access to camera capabilities and controls that are hidden behind normal camera applications.

This series will guide you from understanding smartphone cameras to building professional-level Camera2 applications. Let’s begin the journey.
