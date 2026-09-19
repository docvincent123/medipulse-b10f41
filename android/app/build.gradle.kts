plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.quremed.medtime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quremed.medtime"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

val generatedMedTimeRes = layout.buildDirectory.dir("generated/medtime/res")
android.sourceSets["main"].res.srcDir(generatedMedTimeRes)

val decodeMedicineAlarm by tasks.registering {
    val inputFile = layout.projectDirectory.file("src/main/sound/medicine_alarm.b64")
    val outputFile = generatedMedTimeRes.map { it.file("raw/medicine_alarm.mp3") }

    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        val encoded = inputFile.asFile.readText().trim()
        destination.writeBytes(java.util.Base64.getDecoder().decode(encoded))
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(decodeMedicineAlarm)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
