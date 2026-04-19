plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
