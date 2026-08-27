plugins {
    id("com.android.application")
}

android {
    namespace = "org.soberania.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.soberania.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }

        create("wireguardLab") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".wireguardlab"
            versionNameSuffix = "-wireguard-lab"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
