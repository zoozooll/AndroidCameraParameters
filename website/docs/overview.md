---
sidebar_position: 1
slug: /
description: Overview of the Android Camera Parameters dashboard and its key diagnostic features including hardware level detection and real-time feature tracking.
keywords: [android camera dashboard, hardware level detection, camera diagnostics]
---

# App Overview

This page provides a detailed breakdown of the application's dashboard and key features.

![App Overview](/img/camera_params_feature_graph.png)

## Dashboard Components

### 1. Navigation & Selection
- **Menu Drawer**: Access Privacy Policy, Rate App, and About information via the top-left menu icon.
- **Camera Selection**: Tap the Camera Name or the ID Badge (e.g., "0") to open a dropdown and switch between available lenses (Rear, Front, Ultra-wide, etc.).
- **Bottom Navigation**: Seamlessly switch between **Overview**, **Categories**, **Raw JSON**, and **Favorites**.

### 2. Summary Card
The Summary Card at the top provides the most critical piece of information:
- **Hardware Level**: The Camera2 API support level (LEGACY, LIMITED, FULL, or LEVEL_3). This determines the overall capabilities of the lens.

### 3. Key Features Grid
A visual grid providing instant status for professional-grade features:
- **Resolution & Sensor Size**: Physical characteristics of the sensor.
- **Max Video FPS**: Peak frame rate capabilities.
- **RAW Support**: Indicates if the sensor can output uncompressed data.
- **OIS (Optical Image Stabilization)**: Physical lens stabilization availability.
- **Manual Control**: Status of Manual Exposure and Manual Focus support.
- **Processing**: Support for HDR, Face Detection, and Red-Eye Reduction.

### 4. Categorized Parameters (Categories Tab)
Explore the full list of CameraCharacteristics organized into logical groups:
- **Sensor**: Resolution, physical size, sensitivity ranges.
- **Lens**: Focal length, aperture, stabilization modes.
- **AE/AF/AWB**: Detailed control modes for exposure, focus, and white balance.
- **Search**: Use the integrated search bar to quickly find specific API keys or values.
