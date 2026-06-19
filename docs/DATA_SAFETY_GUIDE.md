# Google Play Data Safety Form Guide

Use the following information to fill out the **Data Safety** section in the Google Play Console for the Camera Parameters app.

## Section 1: Data collection and security

| Question | Answer |
| :--- | :--- |
| Does your app collect or share any of the required user data types? | **No** |

*Note: Since you select "No", most of the following sections will be skipped or automatically answered.*

## Section 2: Data collection declaration (if asked for specifics)

Even though the app uses the `CAMERA` permission, it does **not** collect data. Google defines "collection" as transmitting data off the device. 

- **Camera**: Select "No" for collection. The app only accesses technical parameters locally.
- **Personal Info**: Select "No".
- **Device Identifiers**: Select "No".

## Section 3: Data sharing

| Question | Answer |
| :--- | :--- |
| Is all of the user data collected by your app encrypted in transit? | **N/A** (No data collected) |
| Do you provide a way for users to request that their data is deleted? | **N/A** (No data collected) |

## Justification for Camera Permission

If the Play Console asks for a justification for the `CAMERA` permission:
> "The app is a camera diagnostic tool that uses the Camera2 API to display technical hardware specifications (e.g., sensor size, focal lengths) to the user. It does not capture or transmit any image or video data."

---

## Important Final Step: Privacy Policy URL
Ensure you provide the link to your public Privacy Policy in the Play Console. If hosting on GitHub, the link should look like:
`https://github.com/zoozooll/AndroidCameraParameters/blob/master/PRIVACY_POLICY.md`
