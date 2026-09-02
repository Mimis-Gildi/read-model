plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.kotlin.serialization) apply false
    alias(ktor_libs.plugins.ktor) apply false

    alias(libs.plugins.compose.compiler) apply false

    alias(libs.plugins.kobweb.application) apply false
    alias(libs.plugins.kobwebx.markdown) apply false

    alias(libs.plugins.kotlinx.kover) apply false
}
