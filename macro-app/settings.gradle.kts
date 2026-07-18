pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TouchMacro"

// Android アプリ本体
include(":app")

// 純JVM の core モジュールは独立ビルド。composite build として取り込む。
// これにより core 単体テスト（Android SDK 不要）と、アプリからの利用を両立する。
// core の座標は group=com.wadop.touchmacro / artifact=core（rootProject.name）。
// app が implementation("com.wadop.touchmacro:core") を宣言すると Gradle が自動置換する。
includeBuild("core")
