import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("dagger.hilt.android.plugin")
    alias(notation = libs.plugins.kotlin.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.protobuf") version "0.9.4"
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "fr.harmoniamk.statsmkworld"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.harmoniamk.statsmkworld"
        minSdk = 28
        targetSdk = 35
        versionCode = 20
        versionName = "3.0.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "DISCORD_API_SECRET", properties.getProperty("DISCORD_API_SECRET"))
        buildConfigField("String", "DISCORD_API_CLIENT", properties.getProperty("DISCORD_API_CLIENT"))
        ksp {
            arg(k = "room.schemaLocation", v = "$projectDir/schemas")
        }

    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            properties.load(project.rootProject.file("local.properties").inputStream())
            storeFile  = file("/Users/pascal/Documents/statsmkworld_keystore")
            storePassword  = "Harmonia2025!"
            keyPassword =  "Harmonia2025!"
            keyAlias =  "statsmkworld"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            enableUnitTestCoverage = false
            manifestPlaceholders["appLabel"] = "Stats MKWorld"
            buildConfigField("boolean", "IS_DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Stats MKWorld (Dev)"
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }
    dataBinding {
        enable = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.ui.tooling.preview.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // JetPack COMPOSE
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.coil.compose)


    //Firebase
    implementation(libs.firebase.bom)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.ui.auth)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)


    // Hilt
    implementation(libs.dagger.hilt.android)
    debugImplementation(libs.androidx.ui.tooling)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)

    //OkHttp - Retrofit
    implementation(dependencyNotation = libs.okhttp)
    implementation(dependencyNotation = libs.retrofit2.retrofit)
    implementation(dependencyNotation = libs.logging.interceptor)
    implementation(dependencyNotation = libs.squareup.okhttp)
    // Moshi
    implementation(dependencyNotation = libs.moshi)
    implementation(dependencyNotation = libs.moshi.kotlin)
    implementation(dependencyNotation = libs.moshi.adapters)
    implementation(dependencyNotation = libs.converter.moshi)
    ksp(dependencyNotation = libs.moshi.kotlin.codegen)

    //Room
    implementation( libs.androidx.room.runtime)
    implementation( libs.androidx.work.runtime)
    implementation( libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(dependencyNotation = libs.work.runtime)

    implementation(libs.accompanist.pager)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
    //Serialization
    implementation(libs.kotlinx.serialization.json)
    ksp(dependencyNotation = libs.kotlin.metadata)

    // Splashscreen
    implementation(libs.androidx.core.splashscreen)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation(dependencyNotation = libs.androidx.material)

}
protobuf {
    protoc {
        artifact = if (osdetector.os == "osx") {
            val arch = System.getProperty("os.arch")
            val suffix = if (arch == "x86_64") "x86_64" else "aarch_64"
            "${libs.google.protobuf.protoc.get()}:osx-$suffix"
        } else
            libs.google.protobuf.protoc.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}



