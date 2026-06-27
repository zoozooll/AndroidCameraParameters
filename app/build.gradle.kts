import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 1. Load Versioning from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val vCode = localProperties.getProperty("VERSION_CODE")?.toInt() ?: 3
val vName = localProperties.getProperty("VERSION_NAME") ?: "1.0.1"

android {
    namespace = "com.aaron.cameraparams"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.minininja.cameraparams"
        minSdk = 23
        targetSdk = 37
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("googlePlay") {
            dimension = "distribution"
            buildConfigField("String", "STORE_NAME", "\"Google Play\"")
        }
        create("fdroid") {
            dimension = "distribution"
            versionNameSuffix = "-fdroid"
            buildConfigField("String", "STORE_NAME", "\"F-Droid\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

// 2. Task to increment version
tasks.register("incrementVersion") {
    doLast {
        if (localPropertiesFile.exists()) {
            val props = Properties()
            localPropertiesFile.inputStream().use { props.load(it) }

            val oldCode = props.getProperty("VERSION_CODE")?.toInt() ?: 0
            val newCode = oldCode + 1
            props.setProperty("VERSION_CODE", newCode.toString())

            val oldName = props.getProperty("VERSION_NAME") ?: "1.0.0"
            val parts = oldName.split(".")
            val newName = if (parts.size >= 3) {
                val lastPart = parts.last().toIntOrNull() ?: 0
                parts.dropLast(1).joinToString(".") + "." + (lastPart + 1)
            } else {
                "$oldName.1"
            }
            props.setProperty("VERSION_NAME", newName)

            localPropertiesFile.outputStream().use {
                props.store(it, "Incremented after successful release build")
            }
            println("Version updated to: $newName ($newCode)")
        }
    }
}

// 3. Task to rename artifacts (APK and AAB)
tasks.register("renameReleaseBundle") {
    doLast {
        val requestedTasks = gradle.startParameter.taskNames

        val isBundleBuild = requestedTasks.any { it.contains("bundle") }
        println("isBundleBuild: $isBundleBuild")
        val isAssembleBuild = requestedTasks.any { it.contains("assemble") }
        println("isAssembleBuild: $isAssembleBuild")

        val destinationFolder1 = project.findProperty("android.injected.bundle.destination.directory")
        println("destinationFolder1: $destinationFolder1")
        val destinationFolder2 = project.findProperty("android.injected.apk.destination.directory")
        println("destinationFolder2: $destinationFolder2")
            // Fallback to the older names just in case
        val destinationFolder3 = project.findProperty("android.injected.bundle.export.dir")
        println("destinationFolder3: $destinationFolder3")
        val destinationFolder4 = project.findProperty("android.injected.apk.export.dir")
        println("destinationFolder4: $destinationFolder4")


//        destDir.walkTopDown().forEach { file ->
//            // Match files that contain "-release" (the default Gradle output naming)
//            // This includes both .apk and .aab files
//            if ((file.extension == "aab" || file.extension == "apk") &&
//                file.name.contains("-release") &&
//                file.absolutePath.contains("release", ignoreCase = true)) {
//
//                // Replace "-release" with "-version-timestamp"
//                // This preserves the original extension (.aab -> .aab, .apk -> .apk)
//                val newName = file.name.replace("-release", "-${vName}-${timestamp}")
//                val dest = File(file.parentFile, newName)
//
//                file.copyTo(dest, overwrite = true)
//                println("Artifact processed: ${file.name} -> ${dest.name}")
//            }
//        }
    }
}

// 4. Hook everything up
tasks.configureEach {
    if (name.startsWith("assemble") && name.endsWith("Release") && !name.contains("Debug")
        && name.contains("GooglePlay")) {
        finalizedBy("incrementVersion")
        finalizedBy("renameReleaseBundle")
    }
//    if (name.startsWith("bundle") && name.endsWith("Release") && !name.contains("Debug")
//        && name.contains("GooglePlay")) {
//        finalizedBy("incrementVersion")
//        finalizedBy("renameReleaseBundle")
//    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("net.lingala.zip4j:zip4j:2.8.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}