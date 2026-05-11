plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":scaffold:core"))
    api(project(":service:feed:api"))
    api(project(":biz:story:background:core"))
    api(project(":biz:story:message:core"))
    api(project(":biz:story:infobar:core"))
    api(project(":biz:story:input:core"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
