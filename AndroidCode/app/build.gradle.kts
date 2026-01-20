plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.micode.notes"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "net.micode.notes"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES");
        resources.excludes.add("META-INF/NOTICE");
        resources.excludes.add("META-INF/LICENSE");
        resources.excludes.add("META-INF/LICENSE.txt");
        resources.excludes.add("META-INF/NOTICE.txt");
        resources.excludes.add("org/apache/commons/codec/language/**");
        resources.excludes.add("org/apache/http/client/version.properties");
        resources.excludes.add("mozilla/public-suffix-list.txt");
        resources.excludes.add("org/apache/http/entity/mime/version.properties");
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(fileTree(mapOf(
        "dir" to "D:\\Android\\AndroidCode\\httpcomponents-client-4.5.14-bin\\lib",
        "include" to listOf("*.aar", "*.jar"),
        "exclude" to listOf("httpclient-4.5.14.jar","commons-codec-1.11.jar","httpclient-cache-4.5.14.jar","fluent-hc-4.5.14.jar","httpmime-4.5.14.jar")
    )))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}