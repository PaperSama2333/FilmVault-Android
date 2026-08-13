# Android 签名与 Tag Release 配置

工作流 `.github/workflows/release.yml` 在推送 `v*.*.*` Tag 时运行，并在发布前依次执行单元测试、Release Lint、R8 压缩、APK 签名校验和 ABI 校验。它分别生成 `arm64-v8a` 与 `x86_64` APK，每个 APK 只允许包含自己的 ABI。

## 1. 在本机生成发布密钥

请把密钥保存在安全的密码管理器或离线备份中。丢失后无法用同一签名升级已安装版本。

```bash
keytool -genkeypair -v \
  -keystore filmvault-release.jks \
  -alias filmvault \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

不要把 `.jks`、密码或 `keystore.properties` 提交到 Git。

## 2. 把密钥转换为单行 Base64

Windows PowerShell：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("filmvault-release.jks")) | Set-Clipboard
```

macOS：

```bash
base64 < filmvault-release.jks | tr -d '\n' | pbcopy
```

Linux：

```bash
base64 -w 0 filmvault-release.jks
```

## 3. 配置 GitHub Actions Secrets

打开仓库：`Settings → Secrets and variables → Actions → New repository secret`，创建以下四项：

| Secret | 内容 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | 上一步得到的完整单行 Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | `filmvault`，或生成时使用的 alias |
| `ANDROID_KEY_PASSWORD` | alias 对应的密钥密码 |

这些值必须放在 **Secrets**，不要放在 Variables。工作流不会打印密钥或密码。

## 4. 推送 Tag 触发签名发布

确认 `main` 的 Android CI 已通过后执行：

```bash
git switch main
git pull --ff-only
git tag -a v1.0.0 -m "FilmVault Android v1.0.0"
git push origin v1.0.0
```

在仓库的 `Actions → Signed multi-ABI Release` 查看进度。成功后 GitHub Releases 会出现：

- `FilmVault-1.0.0-arm64-v8a.apk`
- `FilmVault-1.0.0-arm64-v8a.apk.sha256`
- `FilmVault-1.0.0-x86_64.apk`
- `FilmVault-1.0.0-x86_64.apk.sha256`

## 5. 后续版本

每个发布版本使用新 Tag，例如 `v1.0.1`、`v1.1.0`。不要移动或复用已经发布过的 Tag，也不要更换 keystore。
