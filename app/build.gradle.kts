plugins { id("com.android.application") }

android {
    namespace = "dev.ryu4696.hitandblow"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.ryu4696.anatomydiffusion"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation("com.google.mediapipe:tasks-vision-image-generator:0.10.26.1")
}
