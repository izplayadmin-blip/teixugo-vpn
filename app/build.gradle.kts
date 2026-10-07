plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.teixugovpn.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.teixugovpn.app"
        minSdk = 23          // WireGuard-android exige 21+; 23 cobre aparelhos Android 6+ para testes.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    // --- Compose / UI ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // --- WireGuard oficial para Android ---
    implementation("com.wireguard.android:tunnel:1.0.20230706")

    // --- Nostr: assinatura Schnorr/secp256k1 (mesma lib usada pelo Amethyst) ---
    // ATENÇÃO: não consegui compilar este projeto neste ambiente pra confirmar
    // a versão exata/nomes de método -- veja o comentário no topo de NostrCrypto.kt.
    implementation("fr.acinq.secp256k1:secp256k1-kmp:0.16.0")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-android:0.16.0")

    // --- Rede (fala com a mesma API PHP do site) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Armazenamento seguro da chave privada no dispositivo ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Tema base (o AndroidManifest referencia um parent do Material Components) ---
    implementation("com.google.android.material:material:1.12.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
