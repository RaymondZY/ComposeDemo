plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":service:user-center:api"))
    implementation(libs.kotlinx.coroutines.core)
}
