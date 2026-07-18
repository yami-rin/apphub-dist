# TouchMacro — Android タッチ操作マクロアプリ

ユーザーが画面上で行った物理的なタッチ操作を、座標・軌跡・指の本数・操作時間・待機時間として扱い、同一端末上で再生することを目的としたアプリです。UI要素・ボタンの意味・入力文字列・画面内容は解析しません（文字入力もソフトウェアキーボード上のタッチとして扱います）。

> **重要:** 本リポジトリ `apphub-dist` はアプリ配布カタログ用のため、本アプリのソースは `macro-app/` 配下にあります。

---

## 1. 最優先の技術検証（結果）

Android 公開 API（非root）で「他アプリ上のユーザー物理タッチを妨げずに記録し、再生する」ことが可能かを検証しました。**結論から言うと、再生は実現可能ですが、録画（他アプリ上の物理タッチを妨げず取得）は公開 API では原理的に不可能です。** コードで実装したように見せかけることはせず、事実を記載します。

### 1-1. 実現できた機能

| 機能 | 実現方法 | 備考 |
|---|---|---|
| ジェスチャー再生（タップ/長押し/スワイプ/ドラッグ/多指/ピンチ） | `AccessibilityService#dispatchGesture` + `GestureDescription`（複数ストローク） | API 24+。多指は複数ストロークで表現。`GestureBuilder` 実装済み |
| 操作の共通データ表現 | 全操作を「複数指の時刻付き座標列」(`CoordinatePayload`) として保持 | タップ/スワイプ/ピンチを別形式にしない |
| 操作パネルのオーバーレイ表示 | `TYPE_APPLICATION_OVERLAY`（小さなパネルのみ） | パネル外は透過し配下アプリを妨げない |
| オーバーレイ内操作を録画/一時停止対象外にする | 自前 View のイベントのみを扱う | 自明に実現可能 |
| マクロ生成タッチと（オーバーレイ上の）ユーザー操作の区別 | 自分が dispatch したかどうかを状態機械が保持 | 下記 1-3 の制限に注意 |
| 再生の一時停止・再開・繰り返し・ループ間待機・状態管理 | 純JVM の状態機械 `PlaybackStateMachine` | 単体テスト済み |
| オーバーレイ面上での多指タッチ座標取得（検証用） | 全画面透過 View の `MotionEvent` 解析（`RecorderPrototype`） | **取得は可能。ただし下記 1-2 の致命的制約あり** |

### 1-2. 実現できなかった機能（公開 API の限界）

**他アプリ上のユーザー物理タッチを「対象アプリの操作を妨げずに」記録すること。**

- `AccessibilityService` は座標付きの生タッチ列を受け取れません（受け取れるのは意味イベントで、座標軌跡は含まれません）。`onGesture`（`AccessibilityGestureEvent`）は **Touch Exploration 有効時のみ**動作し、タッチを横取りするため対象アプリの通常操作を妨げます。
- オーバーレイ View で `MotionEvent` を取得する方法は、
  - `FLAG_NOT_TOUCHABLE`（透過）にすると **座標を取得できない**、
  - タッチ可能にすると **配下アプリにタッチが届かない（妨げる）**、
  のどちらかにしかならず、**「取得」と「無干渉」は両立できません**。
- 生入力（`/dev/input`）の読み取りは **root**、または **ADB `getevent`（PC接続）** が必要で、いずれも一般アプリの公開 API では使えません。

本アプリの `RecorderPrototype` は「オーバーレイ面上でなら多指の時刻付き座標を取得できる」ことを実証しますが、その間タッチは配下アプリへ届きません。したがって**無干渉録画としては使用できず、取得可能性の検証用**という位置づけです。

**代替案（無干渉録画が必要な場合）:**
1. **root 端末**で `/dev/input/eventX` を読む（`su` 前提。公開 API 外）。
2. **ADB 経由**で PC から `getevent` して記録し、アプリへ取り込む（開発/検証用途）。
3. OEM/MDM の**特権 API**（一般配布アプリでは不可）。
4. 記録は諦め、**手動で座標マクロを定義 → `dispatchGesture` で再生**する運用に振る（再生は公開 API だけで完結）。

### 1-3. 「物理タッチでの一時停止」に関する制限

仕様では「再生中、再生用オーバーレイ以外への物理タッチで一時停止」を求めていますが、**配下アプリ上の任意のタッチを検知するには全画面タッチ捕捉ビューが必要で、それを重ねると `dispatchGesture` で注入したジェスチャー自身も吸われて再生が成立しません**（1-2 と同根の制約）。

このため本アプリでは、一時停止は**再生用オーバーレイの一時停止ボタン**（ユーザーの明示的な物理タッチ）で行います。状態機械側（`PlaybackStateMachine`）は「物理タッチによる一時停止」を一般化して実装済みなので、将来 root 等で全画面のタッチ検知が可能になれば、そのイベントを `onPhysicalUserTouch` に接続するだけで仕様どおりの挙動になります。

### 1-4. 対応 Android バージョン

- **minSdk = 26（Android 8.0 Oreo）**
  - 理由: 多指の連続ジェスチャー（`dispatchGesture` の複数ストローク/`willContinue`）と `TYPE_APPLICATION_OVERLAY` が安定して使えるため。
- **targetSdk / compileSdk = 34（Android 14）**
- Android 13（API 33）以降は通知に実行時権限（`POST_NOTIFICATIONS`）が必要。
- Android 14（API 34）以降のフォアグラウンドサービスは `specialUse` タイプを宣言。

### 1-5. 使用した Android API

- `android.accessibilityservice.AccessibilityService`（`dispatchGesture`, `GestureResultCallback`）
- `android.accessibilityservice.GestureDescription` / `GestureDescription.StrokeDescription`
- `android.view.WindowManager` + `TYPE_APPLICATION_OVERLAY`（オーバーレイ）
- `android.view.MotionEvent`（検証用の座標取得）
- `Settings.canDrawOverlays` / `ACTION_MANAGE_OVERLAY_PERMISSION`、`ENABLED_ACCESSIBILITY_SERVICES`
- `PowerManager#isIgnoringBatteryOptimizations`、`POST_NOTIFICATIONS`
- `Intent.ACTION_SCREEN_OFF`（画面消灯検知）
- Jetpack: Compose(Material3), Room, Hilt, DataStore, Coroutines/Flow

### 1-6. 既知の制限

- 無干渉録画は不可（1-2）。`RecorderPrototype` は検証用。
- 配下アプリ上の物理タッチによる自動一時停止は不可（1-3）。オーバーレイの一時停止ボタンで代替。
- `dispatchGesture` は送信済みジェスチャーを途中キャンセルできない。一時停止時は状態機械のガードで、遅延して届く完了通知を無視する（二重実行防止）。
- 多指は端末の `GestureDescription.getMaxStrokeCount()`（一般に10）まで。総時間にも上限があり、超過分はクランプ。
- 座標は録画端末と同一解像度での再生を前提（座標を解釈・変換しない）。
- 対象アプリ内で意図した結果になったかは判定しない（送信完了＝操作完了とみなす）。

### 1-7. 実機で確認すべき項目

このリポジトリの実行環境には Android SDK も実機もないため、以下は**実機での確認が必要**です（コード上は公開 API の契約に沿って実装済み）。

1. `dispatchGesture` によるタップ/長押し/スワイプ/ドラッグ/2本指ピンチ/多指の再生精度と、対象アプリでの受理。
2. ソフトウェアキーボード上のタップ/フリック相当の再生（キーボードは通常の座標タッチとして扱う）。
3. `TYPE_APPLICATION_OVERLAY` パネルが配下アプリの操作を妨げないこと（パネル外の透過）。
4. `RecorderPrototype` の全画面捕捉が確かにタッチを消費し配下へ届かないこと（＝1-2 の制約の実機確認）。
5. 画面消灯（`ACTION_SCREEN_OFF`）での再生停止と、録画の一時保存/復旧。
6. `GestureResultCallback` の `onCancelled` 発生時のエラー記録と一時停止。
7. 端末ごとの `getMaxStrokeCount()` / 最大ジェスチャー時間の差異。
8. Android 13/14 での通知権限・フォアグラウンドサービス種別の挙動。

---

## 2. ビルド/実行環境に関する注記

このリポジトリを作成した CI コンテナには **Android SDK が導入できません**（配布元 `dl.google.com` が組織のネットワークポリシーで遮断されているため）。そのため **APK ビルド（`assembleDebug`）と実機検証はこの環境では実行できません**。

一方、ドメインロジックは Android 非依存の純JVM モジュール `:core` に隔離しており、**この環境で実際に Gradle ビルド＆ユニットテストを実行し、全テストの成功を確認済み**です。

- `:core` … 純JVM（`kotlin("jvm")`）。**SDK 不要でビルド/テスト可能**。
- `:app` … Android アプリ。**ビルドには Android Studio + Android SDK が必要**（composite build で `:core` を取り込みます）。

---

## 3. プロジェクト構成

```
macro-app/
├── settings.gradle.kts          # :app を include、core を includeBuild（composite）
├── build.gradle.kts             # ルート（プラグイン登録のみ）
├── gradle/libs.versions.toml    # バージョンカタログ
├── core/                        # ★純JVM。SDK 不要でテスト可能
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/wadop/touchmacro/core/
│       │   ├── model/           # Operation, Recording, MacroUnit ほか（拡張可能な操作タイプ）
│       │   ├── playback/        # PlaybackStateMachine, LoopController, WaitMath, PlaybackProgram
│       │   ├── unit/            # UnitBuilder（コピー方式）
│       │   └── util/            # RecordingNaming（通番採番）
│       └── test/kotlin/...      # 全ドメインの単体テスト
└── app/                         # Android アプリ
    └── src/main/java/com/wadop/touchmacro/
        ├── data/                # Room（entities/dao/db）, 直列化, mapper, repository
        ├── domain/              # repository インターフェース
        ├── service/             # AccessibilityService, PlaybackController, GestureBuilder,
        │                        #   OverlayController, RecorderPrototype(検証用), FGS
        ├── ui/                  # Compose（list/settings/unit/permission/record） + ViewModel
        ├── di/                  # Hilt モジュール
        └── util/                # PermissionChecker, Formatters
```

### レイヤ分離

- **UI**: Jetpack Compose + Material3 + ViewModel（`ui/`）
- **ドメイン**: 純JVM の `:core`（機種非依存ロジック）＋ `domain/`（リポジトリ抽象）
- **データ**: Room + kotlinx.serialization（`data/`）
- **Android サービス**: AccessibilityService / オーバーレイ / FGS（`service/`）

---

## 4. 再生状態機械

明示的な状態機械（`PlaybackStateMachine`）として実装。想定状態は
`Idle / Preparing / Playing / WaitingBetweenOperations / WaitingBetweenLoops / PausedDuringGesture / PausedDuringWait / Completed / Stopped / Error`。

- 各イベントは「その状態でのみ有効」なガードを持ち、一時停止・再開・終了・画面消灯・サービス停止が競合しても**二重実行しません**。
- 待機中の一時停止は残り待機時間を保存し、再開後に残りを消化してから次へ進みます。
- 操作実行途中の一時停止は当該操作を中断し、再開後は次操作へ（中断操作は再実行しない）。

---

## 5. データモデルの要点

- 1操作 = 最初の指が触れてから全指が離れるまで。操作ID・開始時刻・継続時間・録画開始からの経過・各指のPointer ID・時刻付き座標列・完了/キャンセル状態・次操作までの待機時間を保持。
- タップ/スワイプ/ピンチを別形式にせず、共通の複数指軌跡（`CoordinatePayload`）で保存。
- 操作タイプは `sealed interface OperationPayload` で拡張可能（将来の UI 要素記録方式を追加しやすい設計）。
- ユニットは**コピー方式**。追加時に操作データを完全複製するため、元マクロを編集/削除しても影響しません。
- 録画初期名称は "YYYYMMDD-通番"。通番は日付ごとに1開始、削除番号は再利用しません（発行済み最大値を保持）。

---

## 6. テスト

`:core`（この環境で実行可能）:

- データモデル（`DataModelTest`）
- 通番採番（`RecordingNamingTest`）
- 再生回数計算・ループ間待機（`LoopControllerTest`）
- 一時停止時の残り時間計算（`WaitMathTest`）
- ユニットのコピー方式／元マクロ削除後の保持（`UnitBuilderTest`）
- 再生プログラム平坦化（`PlaybackProgramTest`）
- 再生状態機械（`PlaybackStateMachineTest`）

`:app`（Android 環境で実行）:

- 操作データ JSON 直列化の往復（`OperationSerializationTest`, JVM 単体）
- Room マイグレーション骨組み（`MigrationTest`, instrumented。`exportSchema=true`）

### この環境で core をテストする

```bash
cd macro-app/core
gradle test        # または ../gradlew :core:test 相当（core は独立ビルド）
```

（本リポジトリ作成時に全 core テストの成功を確認済み。）

---

## 7. アプリのビルド手順（Android Studio）

1. `macro-app/` を Android Studio で開く（`local.properties` に SDK パスを設定）。
2. `minSdk 26 / compileSdk 34` を含む SDK Platform をインストール。
3. Gradle 同期 → `:app` を実行。
4. 初回起動で「初期設定」画面から権限を許可:
   - ユーザー補助サービス（必須・再生に使用）
   - 他のアプリ上への表示（必須・オーバーレイ）
   - 通知（Android 13+）
   - バッテリー最適化の除外（任意）

コマンドラインで検証する場合（SDK 導入済み環境）:

```bash
cd macro-app
./gradlew :core:test          # 純JVM テスト
./gradlew :app:assembleDebug  # APK ビルド（Android SDK 必須）
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
```

---

## 8. 権限とエラー処理

- 必須権限が不足している場合、録画/再生は開始せず、設定画面へ誘導します。
- 画面消灯・サービス停止・ジェスチャーキャンセル・データ破損を状態機械とリポジトリで扱い、クラッシュしないよう例外処理しています（`dispatchGesture` の `onCancelled` はエラー記録＋一時停止）。
