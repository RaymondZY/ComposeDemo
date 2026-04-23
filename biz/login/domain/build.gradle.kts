plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":scaffold:core"))
    api(project(":service:user-center:api"))
    api(project(":service:storage:api"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":service:user-center:mock"))
    testImplementation(project(":service:storage:mock"))
}
