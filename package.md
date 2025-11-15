# 如何打包?

本项目使用Android Studio打包。  

在 Android Studio 中指定版本并打包应用程序通常涉及以下几个步骤：

### 1. 指定应用版本

在 Android Studio 中，应用的版本信息通常在 `build.gradle` 文件中指定。你需要编辑 `app` 模块下的 `build.gradle` 文件，不是根目录下的！！！


```groovy
// ... existing code ...
android {
    // ... existing code ...
    defaultConfig {
        // ... existing code ...
        versionCode 1 // 版本代码，通常用于内部版本管理
        versionName "1.0" // 版本名称，用户可见
        // ... existing code ...
    }
    // ... existing code ...
}
// ... existing code ...
```

- `versionCode` 是一个整数，每次发布新版本时都需要增加。
- `versionName` 是一个字符串，通常用于显示给用户。

### 2. 打包应用

要打包应用程序，你可以使用 Android Studio 的“生成 APK”功能。以下是步骤：

1. **打开 Android Studio** 并加载你的项目。
2. **选择“Build”菜单**，然后选择“Build Bundle(s) / APK(s)”。
3. **选择“Build APK(s)”**，这将开始构建过程。
4. 构建完成后，Android Studio 会在右下角弹出一个通知，提示你 APK 已经生成。
5. 点击通知中的“Locate”按钮，打开 APK 文件所在的目录。

### 3. 签名 APK

在发布应用之前，你需要对 APK 进行签名。以下是签名步骤：

1. **选择“Build”菜单**，然后选择“Generate Signed Bundle / APK...”
2. 选择“APK”，然后点击“Next”。
3. 如果你已经有一个密钥库，选择它并输入相关信息。如果没有，你需要创建一个新的密钥库。
4. 选择构建类型（通常是“Release”）和签名版本。
5. 点击“Finish”开始生成签名的 APK。

完成后，你将获得一个签名的 APK 文件，可以用于发布。

### 4. 发布应用

一旦你有了签名的 APK，你可以将其上传到 Google Play 商店或其他应用分发平台。

希望这些步骤能帮助你成功指定版本并打包你的 Android 应用！

如果有其他问题，请随时问我。