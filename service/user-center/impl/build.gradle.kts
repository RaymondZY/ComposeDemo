plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "zhaoyun.example.composedemo.service.usercenter.impl"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(project(":service:user-center:api"))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
