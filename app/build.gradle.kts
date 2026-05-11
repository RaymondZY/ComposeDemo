plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "zhaoyun.example.composedemo"

    compileSdk = 36

    defaultConfig {
        applicationId = "zhaoyun.example.composedemo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    lint {
        disable += "PropertyEscape"
        abortOnError = false
    }
}

dependencies {
    implementation(project(":biz:home:platform"))
    implementation(project(":biz:feed:platform"))
    implementation(project(":biz:story:platform"))
    implementation(project(":biz:story:background:platform"))
    implementation(project(":biz:story:infobar:platform"))
    implementation(project(":biz:story:comment-panel:platform"))
    implementation(project(":biz:story:share-panel:platform"))
    implementation(project(":biz:story:input:platform"))
    implementation(project(":biz:story:message:platform"))
    implementation(project(":biz:story:story-panel:platform"))
    implementation(project(":service:feed:mock"))
    implementation(project(":service:user-center:api"))
    implementation(project(":service:user-center:impl"))
    implementation(project(":service:storage:impl"))
    implementation(project(":scaffold:platform"))

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(project(":service:user-center:mock"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
