plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pdf.alldocument.office.reader"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

// The bundled Apache POI / dom4j sources mark deprecated members with @deprecated
// javadoc tags only (no @Deprecated annotation), which makes javac emit hundreds of
// [dep-ann] warnings. Silence just that lint category.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-dep-ann")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.ads.mobile.sdk)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
}
