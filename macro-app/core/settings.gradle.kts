// core モジュールを「独立した純JVMビルド」として構成する。
// これにより Android SDK / AGP が無い環境でも core 単体でビルド・テストできる。
// Android アプリ本体（:app）は composite build（includeBuild）で本モジュールを取り込む。
// artifact 名は "core"。app からは "com.wadop.touchmacro:core" として参照される。
rootProject.name = "core"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
