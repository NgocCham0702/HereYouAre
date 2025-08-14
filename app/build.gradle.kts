
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.cham.appvitri"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cham.appvitri"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("com.google.maps.android:maps-compose:4.3.0") // Hoặc phiên bản mới nhất
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")

    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")

    implementation("com.google.firebase:firebase-analytics")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    val navVersion = "2.7.7" // Hoặc phiên bản mới nhất bạn muốn dùng
    implementation("androidx.navigation:navigation-compose:$navVersion")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.android.gms:play-services-auth:21.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    //implementation("com.google.firebase:firebase-storage-ktx") // Dù không dùng Storage, cứ để đó phòng sau này
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // Triển khai Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Triển khai thư viện Authentication (không cần phiên bản)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
secrets {
    // (Tùy chọn) Bạn có thể thay đổi file properties mặc định ở đây.
    // Mặc định, plugin sẽ tự động tìm "local.properties" trong thư mục gốc.
    // propertiesFileName = "local.properties"

    // (Tùy chọn) Bỏ qua một số build types nếu cần
    // ignoreMissingSecrets = true
}