plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.example.temi_test"
    compileSdk = 35
    

    defaultConfig {
        applicationId = "com.example.temi_test"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MQTT_USERNAME", "\"${project.property("MQTT_USERNAME")}\"")
        buildConfigField("String", "MQTT_PASSWORD", "\"${project.property("MQTT_PASSWORD")}\"")
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.kotlinx.coroutines.android)
    implementation(libs.sdk)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation (libs.rtplibrary)
    implementation (libs.okhttp)
    implementation (libs.libvlc.all)
    implementation (libs.java.websocket)
    //view model
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.activity.ktx)
    // Eclipse Paho MQTT Client
    implementation (libs.org.eclipse.paho.client.mqttv3)
    implementation (libs.org.eclipse.paho.android.service)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.gson)








}