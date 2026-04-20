plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
