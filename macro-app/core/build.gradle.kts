import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 純JVM Kotlin ライブラリ。Android には依存しない。
// ここにマクロのドメインモデル・再生状態機械・ループ計算・ユニットコピー等の
// 「機種非依存のロジック」を集約し、JVM 上のユニットテストで検証可能にする。
// バイトコードは JVM 17 を対象にし、Android(:app) から consume できるようにする。
plugins {
    kotlin("jvm") version "2.0.21"
    `java-library`
}

group = "com.wadop.touchmacro"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
