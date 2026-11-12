import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Dados da chave de produção. Ficam em `keystore.properties` (fora do Git) e só existem
 * no computador de quem publica. Sem o arquivo (ex.: no GitHub Actions), o build de
 * produção sai sem assinatura — o debug funciona normalmente.
 */
val assinatura = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "br.com.fashiondog"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.fashiondog"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Nome que aparece embaixo do ícone (cada tipo de build troca, ver abaixo).
        manifestPlaceholders["nomeDoApp"] = "FashionDog"
    }

    signingConfigs {
        if (!assinatura.isEmpty) {
            create("producao") {
                storeFile = file(assinatura.getProperty("storeFile"))
                storePassword = assinatura.getProperty("storePassword")
                keyAlias = assinatura.getProperty("keyAlias")
                keyPassword = assinatura.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Desenvolvimento: outro app no celular ("FashionDog (teste)"), para nunca
        // sobrescrever o app de verdade nem misturar dados de teste com os reais.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["nomeDoApp"] = "FashionDog (teste)"
        }
        // Produção: otimizado (R8) e assinado com a chave da FashionDog.
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            signingConfig = signingConfigs.findByName("producao")
        }
        // Homologação: igual à produção, mas instala ao lado dela para conferir antes de entregar.
        create("homologacao") {
            initWith(getByName("release"))
            applicationIdSuffix = ".homolog"
            versionNameSuffix = "-homolog"
            manifestPlaceholders["nomeDoApp"] = "FashionDog (homolog)"
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
