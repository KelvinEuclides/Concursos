import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

// ----------------------------------------------------------------------------
// Versionamento automático
//   versionName: -PversionName=… (CI, a partir da tag) senão a última tag git
//                (`git describe`), senão "1.0.0".
//   versionCode: -PversionCode=… (CI) senão o nº de commits em HEAD, senão 1.
// ----------------------------------------------------------------------------
fun gitOutput(vararg args: String): String = runCatching {
    providers.exec {
        commandLine(args.toList())
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("")

val autoVersionName: String =
    (providers.gradleProperty("versionName").orNull
        ?: gitOutput("git", "describe", "--tags", "--abbrev=0").removePrefix("v").ifBlank { null }
        ?: "1.0.0")

val autoVersionCode: Int =
    (providers.gradleProperty("versionCode").orNull?.toIntOrNull()
        ?: gitOutput("git", "rev-list", "--count", "HEAD").toIntOrNull()
        ?: 1)

// ----------------------------------------------------------------------------
// Assinatura release
//   Lê de env vars (CI: KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS /
//   KEY_PASSWORD) ou de um keystore.properties na raiz (build local).
//   Sem keystore -> o build release cai na assinatura debug (nunca falha).
// ----------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingSecret(key: String): String? =
    System.getenv(key) ?: keystoreProps.getProperty(key) ?: providers.gradleProperty(key).orNull
val releaseStoreFile: File = rootProject.file(signingSecret("KEYSTORE_FILE") ?: "keystore/release.jks")

jacoco {
    toolVersion = "0.8.12"
}

// Relatório de cobertura dos testes unitários JVM: ./gradlew :app:jacocoTestReport
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val excluded = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "**/*_Impl*.*", "**/databinding/**",
        "**/*ComposableSingletons*.*", "**/Lambda*.class",
        // UI Compose: precisa de testes instrumentados / Compose, fora do âmbito
        // dos testes unitários JVM.
        "**/ui/theme/**", "**/ui/components/**", "**/ui/screens/**", "**/ui/icons/**",
        "**/MainActivity*.*", "**/UfsaApplication*.*",
    )
    // AGP 9 (built-in Kotlin) e AGP mais antigo guardam as classes em sítios
    // diferentes — cobrimos ambos.
    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) { exclude(excluded) },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(excluded) },
        )
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
}

android {
    namespace = "mz.co.kevin.concursos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mz.co.kevin.concursos"
        minSdk = 26
        targetSdk = 37
        versionCode = autoVersionCode
        versionName = autoVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile.exists()) {
                storeFile = releaseStoreFile
                storePassword = signingSecret("KEYSTORE_PASSWORD")
                keyAlias = signingSecret("KEY_ALIAS")
                keyPassword = signingSecret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig =
                if (releaseStoreFile.exists()) signingConfigs.getByName("release")
                else signingConfigs.getByName("debug")
            // R8/shrinking desligado por agora (ver issue #8); assinatura já activa.
            // Regras já prontas — mudar para true quando se quiser activar.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    lint {
        // Falha o build (e o CI) em erros de lint; avisos não bloqueiam.
        abortOnError = true
        warningsAsErrors = false
        // O lint da variante release é redundante no CI (mesma fonte).
        checkReleaseBuilds = false
        sarifReport = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    // Compatibilidade com páginas de memória de 16 KB (Android 15+ / requisito Play).
    // Empacota as .so nativas (MediaPipe) sem compressão e alinhadas a 16 KB.
    // É o comportamento por omissão no AGP moderno; fica explícito para não regredir.
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Scraping e rede
    implementation(libs.jsoup)
    implementation(libs.okhttp)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Background sync
    implementation(libs.androidx.work.runtime.ktx)

    // On-device AI (MediaPipe GenAI / Gemma)
    implementation(libs.mediapipe.tasks.genai)

    testImplementation(libs.junit)
    // org.json real (a versão do android.jar lança "not mocked" nos testes JVM)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
