plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":scaffold:core"))
    api(project(":service:feed:api"))
    api(project(":biz:story:background:domain"))
    api(project(":biz:story:message:domain"))
    api(project(":biz:story:infobar:domain"))
    api(project(":biz:story:input:domain"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
