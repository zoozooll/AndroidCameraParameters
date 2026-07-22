package com.aaron.cameraparams.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.theme.CameraParamsTheme

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.menu_privacy_policy)) },
        text = {
            PrivacyPolicyContent()
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
fun PrivacyPolicyContent() {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Last Updated: June 19, 2026",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = buildAnnotatedString {
                append("This Privacy Policy describes how ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Camera Parameters")
                }
                append(" (\"we\", \"us\", or \"our\") handles information when you use our mobile application.")
            },
            style = MaterialTheme.typography.bodyMedium
        )

        PrivacySection(title = "1. Information Collection and Use") {
            Text(
                text = "The Camera Parameters app is designed as a technical diagnostic tool. Our primary goal is to provide users with transparency regarding their device's hardware capabilities.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Camera Metadata Access",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = buildAnnotatedString {
                    append("The app accesses camera metadata via the Android ")
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)) {
                        append("Camera2")
                    }
                    append(" API.")
                },
                style = MaterialTheme.typography.bodyMedium
            )

            BulletPoint("No Permission Required: We have designed the app to run without the android.permission.CAMERA permission.")
            BulletPoint("Purpose: We read technical metadata (CameraCharacteristics) such as supported hardware levels and basic lens info. Note that without the camera permission, some advanced hardware details may not be accessible.")
            BulletPoint("No Capture: The app does not capture, record, or store any photos, videos, or audio.")
            BulletPoint("Local Processing: All data read from the camera API is processed locally on your device and displayed on your screen.")
        }

        PrivacySection(title = "2. Data Storage and Transmission") {
            BulletPoint("No Data Collection: We do not collect, store, or transmit any personal information, camera data, or device identifiers.")
            BulletPoint("No Remote Servers: The app does not connect to any remote servers. Your camera data never leaves your device.")
            BulletPoint("No Third-Party Sharing: Since no data is collected, no data is shared with third parties or service providers.")
        }

        PrivacySection(title = "3. Children's Privacy") {
            Text(
                text = "Our app does not collect any personal information and is safe for use by all age groups. We do not knowingly collect personal data from children.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        PrivacySection(title = "4. Changes to This Privacy Policy") {
            Text(
                text = "We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        PrivacySection(title = "5. Contact Us") {
            Text(
                text = "If you have any questions about this Privacy Policy, please contact us at:",
                style = MaterialTheme.typography.bodyMedium
            )

            val emailAnnotated = buildAnnotatedString {
                append("Email: ")
                pushStringAnnotation(tag = "URL", annotation = "mailto:kangkang365@gmail.com")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("kangkang365@gmail.com")
                }
                pop()
            }

            val githubAnnotated = buildAnnotatedString {
                append("Project Site: ")
                pushStringAnnotation(tag = "URL", annotation = "https://github.com/zoozooll/AndroidCameraParameters")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("https://github.com/zoozooll/AndroidCameraParameters")
                }
                pop()
            }

            ClickableText(
                text = emailAnnotated,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    emailAnnotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )

            ClickableText(
                text = githubAnnotated,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    githubAnnotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )
        }
    }
}

@Composable
fun PrivacySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        Text(text = "• ", style = MaterialTheme.typography.bodyMedium)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyPolicyDialogPreview() {
    CameraParamsTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PrivacyPolicyContent()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {}) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
