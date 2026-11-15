import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
    alias(libs.plugins.nmcp)
    signing
}

android {
    namespace = "${Constants.PACKAGE}.pin_lock_compose"
    compileSdk = Constants.COMPILE_SDK

    defaultConfig {
        minSdk = Constants.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {

    // compose ui
    implementation(libs.androidx.ui)

    //test
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // compose preview
    implementation(libs.androidx.ui.tooling.preview)

    // compose activity
    implementation(libs.androidx.activity.compose)

    // compose material3
    implementation(libs.androidx.material3)

    // lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // security
    implementation(libs.androidx.security.crypto)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.zaneschepke"
            artifactId = "pin_lock_compose"
            version = providers.gradleProperty("pinVersionName").get()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("Pin Lock Compose")
                description.set("Light library that is beautiful Pin Lock screen for Jetpack Compose. The library handles saving pin in Encrypted file. Integration is very easy and fast.")
                url.set("https://github.com/zaneschepke/pin_lock_compose")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/zaneschepke/pin_lock_compose")
                    developerConnection.set("scm:git:https://github.com/zaneschepke/pin_lock_compose")
                    url.set("https://github.com/zaneschepke/pin_lock_compose")
                }
                developers {
                    organization {
                        name.set("Zane Schepke")
                        url.set("https://zaneschepke.com")
                    }
                    developer {
                        name.set("Zane Schepke")
                        email.set("support@zaneschepke.com")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        getLocalProperty("SECRET_KEY") ?: System.getenv("SECRET_KEY"),
        getLocalProperty("PASSWORD") ?: System.getenv("PASSWORD")
    )
    sign(publishing.publications)
}