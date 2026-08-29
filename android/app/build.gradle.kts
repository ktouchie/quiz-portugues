plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ktouchie.quizportugues"
    // TODO: bump compileSdk/targetSdk to the latest stable release when building —
    // these were current as of this project's scaffolding and should be kept fresh.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ktouchie.quizportugues"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // A fixed, committed debug key (standard debug-key credentials — not sensitive; this
            // is the conventional throwaway signing identity every AGP project would otherwise
            // auto-generate locally). Without this, AGP falls back to ~/.android/debug.keystore,
            // freshly auto-generated per machine — on GitHub Actions' ephemeral runners that means
            // a NEW, differently-signed key on every single CI run, so the rolling pre-release APK
            // (android-ci.yml) could never be installed over a previous install: Android refuses
            // to update an app whose new APK isn't signed by the same key as what's already on the
            // device ("package conflicts with an existing package"). Pinning this file makes every
            // build from CI (and from any dev machine) share one identity, so updates install
            // cleanly. One-time cost: anyone with an install from before this file existed needs
            // to uninstall once — its key won't match this one either.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    // Exports Room's schema JSON per version to android/app/schemas/, checked into version
    // control — this is what makes the schema-versioning story in the spec (§6.2) real, since it
    // gives Room's migration tests something to validate against as the schema evolves.
    //
    // KNOWN ISSUE: this path isn't variant-scoped, so kspDebugKotlin and kspReleaseKotlin race
    // writing the same file if both run in one invocation (e.g. `./gradlew test` or `build`,
    // which touch every variant) — intermittently fails with "Empty schema file". CI works
    // around it by scoping to `testDebugUnitTest` specifically (android-ci.yml). The real fix is
    // a per-variant schema directory via AGP's variant API; not done yet since only the debug
    // variant is actually exercised anywhere right now.
    arg("room.schemaLocation", "$projectDir/schemas")
}

// The repo-root verbs.json / vocabulary.json are the single source of truth for content (shared
// with the web app — see docs/MOBILE_APP_SPEC.md §4/§6.3). Rather than committing a second copy
// under source control that can drift, copy them into assets/ at build time and gitignore the
// copies (see .gitignore).
val copyContentJson by tasks.registering(Copy::class) {
    from(rootProject.projectDir.parentFile) {
        include("verbs.json", "vocabulary.json", "gender_quiz.json", "ser_estar_ficar.json", "contractions.json", "subjunctive_quiz.json", "indirect_speech.json")
    }
    into("src/main/assets")
}

tasks.named("preBuild") {
    dependsOn(copyContentJson)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Android's bundled org.json classes are stubs on the local unit-test JVM (they throw
    // "not mocked" at runtime). This is the real reference implementation, same package name,
    // so code under src/main using org.json is actually testable under ./gradlew test.
    testImplementation(libs.org.json)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
