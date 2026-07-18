---
sidebar_position: 3
---

# Data Safety Guide

This document outlines the data collection and privacy practices for the Android Camera Parameters application.

## Overview

Android Camera Parameters is designed with a "Privacy First" approach. As a diagnostic tool, it needs to access hardware information to function, but it does not collect or transmit personal data.

## Permissions

### Camera Permission (`android.permission.CAMERA`)
- **Requirement**: Necessary to access the `CameraManager` and retrieve `CameraCharacteristics`.
- **Usage**: The app only reads hardware metadata. It **does not** record video or take photos without explicit user action (e.g., in future versions if image capture testing is added).

## Data Collection

- **Personal Information**: The app **does not collect** names, email addresses, phone numbers, or any other personal identifiers.
- **Location Data**: The app **does not access** your GPS or network location.
- **Hardware Metadata**: The app reads technical specifications of your camera lenses (resolution, focal length, supported modes). This data stays on your device unless you explicitly use the "Export JSON" feature to share it.

## Data Sharing

The application **does not share** any data with third parties. There are no tracking SDKs (like Firebase Analytics or Facebook SDK) integrated into the core app.

## User Control

- **JSON Export**: Users can choose to copy or share the raw camera parameter JSON. This is entirely user-initiated.
- **Permissions**: You can revoke the Camera permission at any time through Android System Settings, though the app will not be able to display camera details without it.
