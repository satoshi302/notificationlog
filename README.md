# 通知ログ (NotificationLog)

Samsung Galaxy などの Android 端末で、**LINE をはじめとするメッセージアプリの通知**を記録し、
**連絡先（会話）ごとにチャット形式**で見返せるアプリです。

<p>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-blueviolet">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-brightgreen">
  <img alt="minSdk 26" src="https://img.shields.io/badge/minSdk-26-orange">
</p>

## できること

- 選んだメッセージアプリ（LINE など）の通知だけを自動で記録
- 会話（連絡先・グループ）ごとにまとめて一覧表示
- タップすると、その会話をチャットの吹き出し形式で時系列表示（グループは送信者名つき）
- 記録するアプリは、端末にインストール済みのアプリ一覧から自由にオン/オフ

## 仕組みと重要な制約（必ずお読みください）

Android では、他アプリの通知を取得する正規の方法は `NotificationListenerService`
（「通知へのアクセス」権限）です。本アプリもこれを使っています。そのため:

- **記録できるのは、権限を許可した後に届いた通知だけです。**
  過去の通知（許可前のもの）は取得できません。これは Android の仕様上の制限です。
  - 補足: Galaxy / Android の標準「通知履歴」機能（設定 → 通知 → 通知履歴）は
    OS 内部にだけ保存され、直近 24 時間程度で消えます。一般アプリからは読み出せません。
- 「通知へのアクセス」はユーザーが手動で許可する必要があります。
- 通知の内容次第で本文が取得できます（ロック画面で内容を隠す設定などでは本文が伏せられる場合あり）。
- 確実に記録し続けるため、Galaxy では本アプリを**電池の最適化の対象外**にすることをおすすめします。
- 端末内の全アプリを一覧表示するため `QUERY_ALL_PACKAGES` 権限を使います。
  個人でサイドロードして使う想定です（Google Play で公開する場合は別途対応が必要）。

## 使い方

1. アプリを起動し、案内に従って「通知へのアクセス」で **通知ログ** をオンにします。
2. 右上の設定（⚙）を開き、記録したいアプリ（例: **LINE**）をオンにします。
3. 以降、対象アプリに届いたメッセージ通知が自動で記録され、ホームの「トーク」に会話として表示されます。
4. 会話をタップするとチャット形式で読めます。

## ビルド方法

このリポジトリには Android SDK は含まれていません。**Android Studio**（推奨）
または Android SDK を導入した環境でビルドしてください。

```bash
# Android Studio で本リポジトリを開いて Run するのが簡単です。
# コマンドラインの場合（ANDROID_HOME / local.properties で SDK を指定済みのこと）:
./gradlew assembleDebug
# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

生成した APK を Galaxy にインストール（提供元不明アプリの許可が必要な場合あり）し、
上記「使い方」の手順で権限を許可してください。

## 動作確認の手順（実機）

1. APK をインストールして起動。
2. 「通知へのアクセス」を許可（通知ログ をオン）。
3. 設定で **LINE** を対象にする。
4. 自分宛に LINE でメッセージを送る（別端末や友だちから）。
5. ホームの「トーク」に会話が現れ、開くとチャット表示されることを確認。

## 技術スタック

- Kotlin / Jetpack Compose (Material 3) / Navigation-Compose
- Room（メッセージ永続化） / DataStore Preferences（対象アプリ設定）
- Kotlin Coroutines / Flow、軽量な手動 DI（`App` が Repository を保持）
- minSdk 26 / target・compile SDK 35

## プロジェクト構成

```
app/src/main/java/com/example/notificationlog/
  App.kt                          … Application + 依存の供給
  service/NotificationLogService  … 通知の受信（NotificationListenerService）
  service/NotificationParser      … 通知 → メッセージ抽出（MessagingStyle 解析）
  data/db/                        … Room（MessageEntity / Dao / AppDatabase / 集計）
  data/prefs/SettingsRepository   … 対象アプリ設定（DataStore）
  data/InstalledAppsRepository    … インストール済みアプリ列挙
  ui/                             … 画面（権限 / トーク一覧 / チャット / 設定）
```

## プライバシー

記録した通知内容は端末内の Room データベースにのみ保存され、外部には一切送信しません。
設定画面から履歴をすべて削除できます。
