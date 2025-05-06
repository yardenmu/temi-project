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
    implementation("com.robotemi:sdk:1.135.1")
    implementation ("androidx.camera:camera-camera2:1.4.2")
    implementation ("androidx.camera:camera-lifecycle:1.4.2")
    implementation ("androidx.camera:camera-view:1.4.2")
    implementation ("com.github.pedroSG94.rtmp-rtsp-stream-client-java:rtplibrary:2.1.9")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("org.videolan.android:libvlc-all:3.4.0")
    implementation ("org.java-websocket:Java-WebSocket:1.5.2")
    // Eclipse Paho MQTT Client
    implementation (libs.org.eclipse.paho.client.mqttv3)
    implementation (libs.org.eclipse.paho.android.service)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.gson)








}