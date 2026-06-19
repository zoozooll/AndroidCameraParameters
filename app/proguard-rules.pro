# Gson rules
-keep class com.google.gson.** { *; }
-keep class com.aaron.cameraparams.ui.CameraParameter { *; }
-keep class com.aaron.cameraparams.ui.ParameterCategory { *; }
-keep class com.aaron.cameraparams.ui.CameraHeaderState { *; }
-keep class com.aaron.cameraparams.ui.CameraOverviewState { *; }
-keep class com.aaron.cameraparams.ui.CameraParametersState { *; }
-keep class com.aaron.cameraparams.ui.UiState { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
