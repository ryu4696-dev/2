plugins { id("com.android.application") }

android {
    namespace = "dev.ryu4696.hitandblow"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.ryu4696.hitandblow"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "16.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
