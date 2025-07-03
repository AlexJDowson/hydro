import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.room)
}

android {
    namespace = "at.florianschuster.hydro"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        applicationId = "at.florianschuster.hydro"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    lint {
        abortOnError = true
    }

    signingConfigs {
        create("release") {
            storeFile =
                if (file("keystore.jks").exists()) {
                    file("keystore.jks")
                } else {
                    null
                }

            val localProperties = gradleLocalProperties(rootDir, providers)

            storePassword = localProperties.getProperty("signingStorePassword")
                ?: System.getenv("SIGNING_STORE_PASSWORD")

            keyAlias = localProperties.getProperty("signingKeyAlias")
                ?: System.getenv("SIGNING_KEY_ALIAS")

            keyPassword = localProperties.getProperty("signingKeyPassword")
                ?: System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (file("keystore.jks").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.animation)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)

    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation(libs.reimagined)
    implementation(libs.reimagined.material3)
    implementation(libs.lottie.compose)

    implementation(libs.browser)
    implementation(libs.datastore.preferences)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.profileinstaller)
    baselineProfile(project(":baselineprofile"))
}
