plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.pdf.alldocument.office.pdfreaderapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pdf.alldocument.office.pdfreaderapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"

//        val props = java.util.Properties().apply {
//            val f = rootProject.file("local.properties")
//            if (f.exists()) f.inputStream().use { load(it) }
//        }
  //      buildConfigField("String", "GEMINI_API_KEY", "\"${props.getProperty("GEMINI_API_KEY", "")}\"")

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
            resValue("string", "app_Id", "\"ca-app-pub-5049280561472321~8542624097\"")
            resValue("string", "banner_Id", "\"ca-app-pub-5049280561472321/2001266472\"")
            resValue("string", "native_id", "\"ca-app-pub-5049280561472321/7062021465\"")
            resValue("string", "interstitial_Id", "\"ca-app-pub-5049280561472321/9688184809\"")
            resValue("string", "appopen_Id", "\"ca-app-pub-5049280561472321/5622815274\"")
            resValue("string", "rewarded_interstitial_id", "\"ca-app-pub-5049280561472321/4969379702\"")
        }
        debug {
            isMinifyEnabled = false
            resValue("string", "app_Id", "\"ca-app-pub-3940256099942544~3347511713\"")
            resValue("string", "banner_Id", "\"ca-app-pub-3940256099942544/2014213617\"")
            resValue("string", "native_id", "\"ca-app-pub-3940256099942544/2247696110\"")
            resValue("string", "interstitial_Id", "\"ca-app-pub-3940256099942544/1033173712\"")
            resValue("string", "appopen_Id", "\"ca-app-pub-3940256099942544/9257395921\"")
            resValue("string", "rewarded_interstitial_id", "\"ca-app-pub-3940256099942544/5354046379\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

// Kotlin 2.2 warns that annotations on constructor value parameters (e.g. Hilt's
// @ApplicationContext on `private val context: Context`) will start targeting the
// backing field/property. Opt in now so the behaviour is stable before it flips.
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Kotlin & Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil & Lottie
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.lottie.compose)

    // PDF Engines (iText + PDFBox)
    implementation(libs.itext.pdf)
    implementation(libs.pdfbox.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.reorderable)
    implementation(project(":allDocsReader"))

    // CameraX
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // ML Kit & Scanner
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CSV Writer
    implementation("org.apache.commons:commons-csv:1.12.0")

    // Gemini Generative AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Monetization & Play Services (GMA Next-Gen SDK)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.play.billing.ktx)
    implementation(libs.play.app.update.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}