plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

group = "com.jem.gameworldjem"
version = "0.1.0"

android {
    namespace = "com.jem.gameworldjem"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.moshi.kotlin)
}

// 🔽 Retrasamos la creación de la publicación hasta que exista el componente "release"
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"]) // ahora ya existe

                groupId = project.group.toString()
                artifactId = "gameworldjem"
                version = project.version.toString()

                pom {
                    name.set("gameworldjem")
                    description.set("Top-down tilemap engine (Compose) with Tiled loader.")
                    url.set("https://github.com/joelmora9618/GameworldJEM")
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/joelmora9618/GameworldJEM")
                credentials {
                    username = System.getenv("GITHUB_USER")
                        ?: (findProperty("gpr.user") as String?)
                    password = System.getenv("GITHUB_TOKEN")
                        ?: (findProperty("gpr.key") as String?)
                }
            }
        }
    }
}
