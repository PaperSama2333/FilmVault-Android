# 胶片匣 FilmVault · Android 原生版

根据 `PaperSama2333/FilmVault-APP` 的功能语义与 UI 重新实现的 Kotlin 原生 Android App。项目保留柯达黄、白底、卡片、筛选胶囊与四栏底部导航，同时采用 Android 13+ 的系统交互。

## 已实现

- 胶卷库存与状态筛选：未拍、拍摄中、待冲洗、已送洗、已完成
- 新增胶卷、项目标签与本卷自定义标签
- 同一胶卷多次拍摄记录、编辑、删除、拍摄日记与多张样张
- Android 系统照片选择器与系统日期选择器
- 冲洗店、送洗日期、扫描状态与状态自动联动
- 已完成胶卷收纳、移出收纳与删除
- 品牌/类型/项目标签的创建、重命名、删除与关联数量
- 月度拍摄量与品牌分布统计
- 本地昵称、头像与系统文件选择器备份/还原
- Jetpack Compose + Material 3 + Navigation 3 + SQLite
- 真实 `arm64-v8a` JNI 标记库、R8 压缩与签名 Release

## Android 与构建基线

- 最低系统：Android 13 / API 33
- 编译 SDK：Android 17 / API 37.1
- 目标 SDK：API 37
- AGP 9.3.1、Gradle 9.5.0、JDK 17
- Compose UI 1.12、Material 3 1.4、Navigation 3 1.1.6
- 输出 ABI：arm64-v8a

Android Studio 安装 Android SDK Platform 37.1、NDK `28.2.13676358` 与 CMake `3.22.1` 后即可同步运行。命令行验证：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

签名发布和四个 GitHub Actions Secrets 的配置见 [docs/signing-release.md](docs/signing-release.md)。
