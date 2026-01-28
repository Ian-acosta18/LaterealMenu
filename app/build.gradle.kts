plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.laterealmenu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.laterealmenu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    // UI y Navegación
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.0")
    implementation("androidx.navigation:navigation-ui:2.7.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // 🔥 SISTEMA DE IA - ML Kit (Google)
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.mlkit:image-labeling-custom:17.0.1")
    implementation("com.google.mlkit:object-detection:17.0.0")

    // Cámara y Procesamiento de Imágenes
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // TensorFlow Lite para modelos personalizados
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")



    // Carga y procesamiento de imágenes
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // Permisos
    implementation("com.karumi:dexter:6.2.3")
    implementation(libs.material)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ML Kit (para fallback)
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    implementation("com.itextpdf:itext7-core:7.2.5")

    // Para notificaciones
    implementation("androidx.work:work-runtime:2.8.1")

    // Para manejo de fechas
    implementation("joda-time:joda-time:2.12.5")
    implementation("androidx.core:core:1.12.0")

    // Para manejo de archivos (opcional pero recomendado)
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
}