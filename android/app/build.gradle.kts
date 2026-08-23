plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// One version number for the website and the app: the repo-root version.txt, which the website
// already shows and .github/workflows/version-bump.yml bumps on every merge to main.
val appVersion: String = rootProject.projectDir.parentFile.resolve("version.txt").readText().trim()
val appVersionCode: Int = appVersion.split(".").let { parts ->
    require(parts.size == 3) { "version.txt must be MAJOR.MINOR.PATCH, was \"$appVersion\"" }
    val (major, minor, patch) = parts.map { it.toInt() }
    // Monotonic as long as minor/patch stay below 1000, so every new build installs as an update.
    major * 1_000_000 + minor * 1_000 + patch
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
        versionCode = appVersionCode
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Committed, standard debug-key credentials (not sensitive). Without this, AGP signs with
            // a per-machine auto-generated key — a new one on every ephemeral CI runner — and Android
            // refuses to install a build over one signed with a different key.
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
        buildConfig = true
    }
}

ksp {
    // Exports Room's schema JSON per version to android/app/schemas/, checked into version
    // control — this is what makes the schema-versioning story in the spec (§6.2) real, since it
    // gives Room's migration tests something to validate against as the schema evolves.
    arg("room.schemaLocation", "$projectDir/schemas")
}

// The repo-root verbs.json / vocabulary.json are the single source of truth for content (shared
// with the web app — see docs/MOBILE_APP_SPEC.md §4/§6.3). Rather than committing a second copy
// under source control that can drift, copy them into assets/ at build time and gitignore the
// copies (see .gitignore).
val copyContentJson by tasks.registering(Copy::class) {
    from(rootProject.projectDir.parentFile) {
        include("verbs.json", "vocabulary.json")
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
