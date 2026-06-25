import dev.iurysouza.modulegraph.gradle.ModuleGraphExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.modulegraph)
}

configure<ModuleGraphExtension> {
    readmePath.set("./README.md")
    heading.set("## Project Module Graph")
}

subprojects {
    afterEvaluate {
        if (pluginManager.hasPlugin("com.google.devtools.ksp")) {
            tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }.configureEach {
                dependsOn(":core:domain:jar")
            }
        }
    }
}
