plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pesenhub.jenggirat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pesenhub.jenggirat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Firebase BoM (Firestore, Auth, Messaging, In-App)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")

    // Volley (Bab 09 & Checklist #19)
    implementation("com.android.volley:volley:1.2.1")

    // OSMdroid (OpenStreetMaps Bab 13 PM & Checklist #22)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // QR Code ZXing (Bab 10 PM & Checklist #25)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    // Location Service
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Supabase (Checklist #33)
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.3")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.3")
    implementation("io.ktor:ktor-client-android:3.0.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
