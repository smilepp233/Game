plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.example.groupproject_game"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.groupproject_game"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    // 添加对SO库的支持
    ndk {
      abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  
  // 添加对本地库的打包支持
  packagingOptions {
    resources {
      excludes += listOf(
        "META-INF/robovm/ios/robovm.xml", 
        "META-INF/DEPENDENCIES", 
        "META-INF/NOTICE",
        "META-INF/LICENSE"
      )
    }
  }
  
  compileOptions {
    sourceCompatibility =JavaVersion.VERSION_11
      targetCompatibility =JavaVersion.VERSION_11
  }
}

dependencies {
  // LibGDX 核心依赖 - 指定版本号


  // 将原生库解压到libs目录
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
  
  // 原有依赖
  implementation (libs.gson)
  implementation (libs.glide)
  implementation(libs.appcompat)
  implementation(libs.material)
  implementation(libs.activity)
  implementation(libs.constraintlayout)
  implementation(libs.gridlayout)
  testImplementation(libs.junit)
  androidTestImplementation(libs.ext.junit)
  androidTestImplementation(libs.espresso.core)
}