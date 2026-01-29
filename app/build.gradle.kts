plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.dagger.hilt.android")
}

ktlint {
    // Exclude androidTest sources from strict ktlint enforcement for now
    filter {
        exclude("**/src/androidTest/**")
    }
}

detekt {
    toolVersion = "1.22.0"
    config = files(rootProject.file("detekt.yml"))
    baseline = file("detekt-baseline.xml")
    buildUponDefaultConfig = true
}

android {
    namespace = "com.headphonetracker"
    compileSdk = 36

    // Read signing properties from project properties or environment variables
    val keystorePath: String? = (project.findProperty("keystorePath") as String?) ?: System.getenv("KEYSTORE_PATH")
    val keystorePassword: String? = (project.findProperty("keystorePassword") as String?) ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias: String? = (project.findProperty("keyAlias") as String?) ?: System.getenv("KEY_ALIAS")
    val keyPassword: String? = (project.findProperty("keyPassword") as String?) ?: System.getenv("KEY_PASSWORD")

    signingConfigs {
        create("release") {
            keystorePath?.let { storeFile = file(it) }
            keystorePassword?.let { storePassword = it }
            keyAlias?.let { this.keyAlias = it }
            keyPassword?.let { this.keyPassword = it }
        }
    }

    defaultConfig {
        applicationId = "com.headphonetracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Enable code shrinking and obfuscation for release builds
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use signing config if provided via properties or environment variables
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // MPAndroidChart for graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt for DI
    implementation("com.google.dagger:hilt-android:2.46.1")
    kapt("com.google.dagger:hilt-compiler:2.46.1")
    // Hilt testing
    testImplementation("com.google.dagger:hilt-android-testing:2.46.1")
    kaptTest("com.google.dagger:hilt-compiler:2.46.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.46.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.46.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.4")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
}
