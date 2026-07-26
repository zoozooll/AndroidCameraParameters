---
sidebar_position: 3
title: "Chapter 3: What Can Modern Smartphone Cameras Do?"
description: Discover the amazing capabilities of modern smartphone cameras including portrait mode, HDR, night mode, RAW capture, and more.
keywords: [smartphone camera features, portrait mode, HDR, night mode, RAW, 8K video]
---

Modern smartphones are capable of photography that was once only possible with professional cameras.

## Introduction

Look at what a smartphone camera can do today:

- **Portrait photos** with professional-looking background blur
- **Night mode** that captures details in near darkness
- **HDR** that preserves both bright highlights and dark shadows
- **8K video** recording
- **Slow-motion** at hundreds of frames per second
- **RAW image capture** for professional editing
- **Macro photography** of tiny objects
- **Telephoto zoom** up to 10x or more
- **Ultra-wide** landscape shots

These features are not magic. They are the result of sophisticated hardware and software working together. And Camera2 gives you control over many of them.

## Portrait Mode

Portrait mode creates beautiful background blur, also known as **bokeh**. This makes the subject stand out dramatically.

How does it work?

Most phones use **multiple cameras** to create depth information. One camera captures the main image while another calculates depth. The software then combines these to blur the background selectively.

Camera2 allows applications to:
- Access multiple cameras simultaneously
- Query depth capabilities
- Control bokeh effects

## HDR — High Dynamic Range

Human eyes can see a much wider range of brightness than a camera sensor. HDR solves this problem by capturing multiple exposures and combining them.

- **Bright exposure** captures highlights
- **Dark exposure** captures shadows
- **Mid exposure** captures details

The ISP merges these into a single image with incredible dynamic range.

Camera2 provides:
- HDR capture modes
- Exposure bracket controls
- Manual exposure settings

## Night Mode

Night mode is one of the most impressive features of modern smartphones. It can capture sharp, detailed photos in near darkness.

The technique is simple in concept:
1. Capture multiple frames
2. Align them to reduce motion blur
3. Combine them to increase light sensitivity
4. Apply noise reduction

Some phones can even capture photos in environments so dark that humans can barely see.

## Slow Motion

Slow-motion video allows you to see moments that happen too quickly for the human eye:
- Water droplets
- Balloon pops
- Sports action
- Nature events

Phones can record at:
- 120 fps — Smooth slow motion
- 240 fps — Standard slow motion
- 480 fps — Extreme slow motion
- 960 fps — Super slow motion

Camera2 exposes high-speed video capabilities through:
- High frame rate configurations
- Special capture requests
- Hardware level requirements

## 8K Video

8K video has four times the resolution of 4K and 16 times the resolution of HD. This allows for:
- Cropping and reframing without quality loss
- Detailed close-ups
- Professional video production

Not all phones support 8K, but Camera2 lets you discover which ones do.

## RAW Capture

RAW images contain all the data captured by the sensor before ISP processing. This gives photographers complete control over:
- Exposure
- White balance
- Contrast
- Saturation
- Noise reduction

RAW files are larger but offer unparalleled editing flexibility.

Camera2 allows:
- RAW image capture
- RAW format selection
- Manual controls for optimal RAW results

## Macro Photography

Macro lenses focus on objects very close to the camera — sometimes just a few centimeters away. This opens up a whole new world of photography:
- Flowers and insects
- Textures and patterns
- Tiny objects

Camera2 lets you:
- Switch between cameras including macro
- Control focus distance
- Adjust lighting for close-ups

## Telephoto

Telephoto lenses bring distant objects closer without physically moving. Flagship phones often have:
- 2x optical zoom
- 3x optical zoom
- Up to 10x hybrid zoom
- Digital zoom up to 100x

Camera2 provides:
- Zoom controls
- Focal length information
- Optical vs digital zoom detection

## Ultra-Wide

Ultra-wide lenses capture a much wider field of view than standard lenses. They are perfect for:
- Landscapes
- Architecture
- Group photos
- Indoor spaces

Some ultra-wide lenses can capture 120 degrees or more.

## Manual Mode

Manual mode gives users complete control over:
- **ISO** — Sensor sensitivity
- **Exposure time** — How long the sensor collects light
- **Focus** — Manual focus control
- **White balance** — Color temperature

Camera2 is the foundation for all manual camera applications.

## Camera2 Can Control It All

The exciting part? **You can control all these features with Camera2.**

By the end of this series, you'll understand:
- Which features your phone supports
- How to enable them programmatically
- How to combine them for stunning results

## Explore with Android Camera Parameters

Before we start coding, check what your phone supports:

1. Open Android Camera Parameters
2. Look for:
   - Available capabilities
   - Zoom levels
   - Frame rates
   - Hardware level
   - RAW support
   - Flash capabilities

You may be surprised by what your device can do!

## Next Chapter

In the next chapter, we'll dive deeper into exploring your phone's camera capabilities. You'll learn how to use Android Camera Parameters to discover:

- Camera IDs and their meanings
- Resolution options
- Frame rate capabilities
- Hardware level
- And much more

This hands-on exploration will make coding with Camera2 much easier.

## Summary

Modern smartphone cameras are incredibly powerful. They can capture portrait photos, HDR images, night shots, slow-motion video, 8K video, RAW images, and much more.

Camera2 provides the tools to control these features programmatically. In the next chapter, we'll explore your phone's specific capabilities using Android Camera Parameters.