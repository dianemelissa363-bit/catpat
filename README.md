# 桌面小猫 (CatPet)

一只纯 Android 原生实现的悬浮桌面宠物。第一版用 🐱 Emoji，零第三方库，不接入 AI。

## 功能
- 悬浮窗显示一只小猫
- 拖动小猫到任意位置
- 点击小猫：换表情 😸 + 弹跳
- 待机：上下轻微漂浮

## 技术栈
- Kotlin 2.0.21 + 经典 View
- AGP 8.5.2 / Gradle 8.7 / JDK 17
- compileSdk 34 / minSdk 26（Android 8.0+）
- 零第三方库

## 构建
在 GitHub Actions 云端编译：push 代码后自动构建 debug APK，产物在 Actions 页面的 Artifacts 里下载。
