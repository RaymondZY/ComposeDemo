plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":service:user-center:api"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":service:user-center:mock"))
}
