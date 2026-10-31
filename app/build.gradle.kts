plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.plime.geneapoche"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.plime.geneapoche"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "0.29"
    }

    signingConfigs {
        if (System.getenv("GP_KEYSTORE") != null) {
            create("release") {
                storeFile = file(System.getenv("GP_KEYSTORE")!!)
                storePassword = System.getenv("GP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("GP_KEY_ALIAS")
                keyPassword = System.getenv("GP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // G1-03 : cle de signature dans les secrets GitHub (GP_KEYSTORE...).
            // Tant que les secrets n'existent pas, repli sur la cle debug (APK installable, non debuggable).
            signingConfig = if (System.getenv("GP_KEYSTORE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
