import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform")
}

group = "com.sallemi"
version = "1.0.0"

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = true

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "252.*"
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.2.5")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("org.json:json:20240303")

    // Ensure we don't package Kotlin stdlib in the plugin (use provided from IntelliJ)
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    compileJava {
        options.release.set(21)
    }

    runIde {
        jvmArgs("-Dide.no.jcef=true")
    }

    buildPlugin {
        archiveBaseName.set("gemini-intellij-plugin")
    }
}
