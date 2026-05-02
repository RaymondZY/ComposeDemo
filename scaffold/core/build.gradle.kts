plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
