plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.mavenPublish)
    signing
}

android {
    namespace = "xyz.teamgravity.pin_lock_compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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

signing {
    val inMemoryKey =
        providers
            .gradleProperty("signingInMemoryKey")
            .orElse(providers.gradleProperty("signing.inMemoryKey"))
    val password =
        providers
            .gradleProperty("signingInMemoryKeyPassword")
            .orElse(providers.gradleProperty("signing.password"))
            .orElse(providers.environmentVariable("GPG_PASS"))
    // In-memory PGP signing uses BouncyCastle and produces .asc files Maven Central
    // rejects. Use gpg on CI instead.
    val useGpgCmd = providers.environmentVariable("SIGNING_USE_GPG_CMD").orNull == "true"
    if (useGpgCmd) {
        extra["signing.gnupg.passphrase"] = password.orNull.orEmpty()
        useGpgCmd()
    } else if (inMemoryKey.isPresent) {
        useInMemoryPgpKeys(inMemoryKey.get(), password.orNull.orEmpty())
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "pin_lock_compose", version.toString())
    pom {
        name = "Pin Lock Compose"
        description = "Light library that is beautiful Pin Lock screen for Jetpack Compose. The library handles saving pin in Encrypted file. Integration is very easy and fast."
        url = "https://github.com/zaneschepke/pin_lock_compose"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "zaneschepke"
                name = "Zane Schepke"
                url = "https://github.com/zaneschepke"
                email = "support@zaneschepke.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/zaneschepke/pin_lock_compose.git"
            developerConnection = "scm:git:ssh://git@github.com/zaneschepke/pin_lock_compose.git"
            url = "https://github.com/zaneschepke/pin_lock_compose"
        }
    }
}
