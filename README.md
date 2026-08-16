# 胶片匣 FilmVault · Android 原生版

根据 `FilmVault-APP` 的功能语义与 UI 重新实现的 Kotlin 原生 Android App。采用 Android 13+ 的系统交互。

## 已实现

- 胶卷库存与状态筛选：未拍、拍摄中、待冲洗、已完成
- 新增胶卷、项目标签与本卷自定义标签
- 同一胶卷多次拍摄记录、编辑、删除、拍摄日记与多张样张
- Android 系统照片选择器与系统日期选择器
- 系统相机拍摄样张，照片直接写入 App 私有目录
- 可选的胶卷冲洗提醒，支持 1、3、7 天延迟
- Navigation 3 原生推入式页面转场与预测返回动画
- Material 3 原生顶部栏、列表项、开关和系统选择器
- 冲洗店、送洗日期、扫描状态与状态自动联动
- 已完成胶卷收纳、移出收纳与删除
- 品牌/类型/项目标签的创建、重命名、删除与关联数量
- 月度拍摄量与品牌分布统计
- 本地昵称、头像与系统文件选择器备份/还原
- Jetpack Compose + Material 3 + Navigation 3 + SQLite
- 独立 `arm64-v8a` / `x86_64` JNI APK、R8 压缩与签名 Release

## Android 与构建基线

- 最低系统：Android 13 / API 33
- 编译 SDK：Android 17 / API 37.1
- 目标 SDK：API 37
- AGP 9.3.1、Gradle 9.5.0、JDK 17
- Compose UI 1.12、Material 3 1.4、Navigation 3 1.1.6
- 输出 ABI：arm64-v8a、x86_64（独立 APK）

Android Studio 安装 Android SDK Platform 37.1、NDK `28.2.13676358` 与 CMake `3.22.1` 后即可同步运行。命令行验证：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

签名发布和四个 GitHub Actions Secrets 的配置见 [docs/signing-release.md](docs/signing-release.md)。
