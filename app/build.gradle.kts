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
        versionCode = 48
        versionName = "0.48"
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
            // G2-13 : R8 actif en release (minification + retrait des
            // ressources inutilisees), regles minimales dans proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    // G2-14 : dependances alignees sur le BOM Compose 2026.08.00 (bump Dependabot).
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    // G2-07 : viewModel() pour porter l'etat (survit a la rotation).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // G3-17 : classes de taille de fenetre (paysage, tablettes).
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-core")
    // G3-05 : ecran de demarrage aux couleurs de l'application.
    implementation("androidx.core:core-splashscreen:1.2.0")
    // G2-10 : tests unitaires JVM.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}


