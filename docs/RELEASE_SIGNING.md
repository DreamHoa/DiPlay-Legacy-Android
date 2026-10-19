# Okavango 签名与发布

包名固定为 `com.shihab.diplay.okavango`，最低 Android 5.1 / API 22。
正式 APK 使用 `release` 构建类型（不可调试），启用 V1 + V2 签名。
`signing-cert.sha256` 固定本项目的签名证书指纹；它不是上游或 Mario 的签名。

## 一次配置

此前 Actions 日志确认未配置持久 debug 签名，旧包的签名随 runner 改变。
APK 只有公钥证书，无法从旧 APK 恢复签名私钥。因此无法保证覆盖此前安装的临时签名包。
首次换为固定签名可能需要卸载旧 Okavango 包；卸载会清除该包设置及配对数据。
以后保留同一个签名密钥，并递增版本号，就可以覆盖安装。

在仓库 Settings → Secrets and variables → Actions 中添加：

| Secret | 内容 |
| --- | --- |
| `OKAVANGO_KEYSTORE_B64` | 签名备份中同名 TXT 文件的完整内容（不要提交到仓库） |
| `DIPLAY_MFI_PK8_B64` | 已配置的授权认证私钥，保持原值 |
| `DIPLAY_MFI_P7B_B64` | 已配置的授权认证证书，保持原值 |

签名备份包含 `okavango-signing.p12`，alias 为 `androiddebugkey`，口令均为 `android`。
这是专用生成的固定密钥；alias/口令沿用 Android 习惯只为简化配置，并不把 release APK 变成 debug APK。
必须保存这份备份。不要重新生成密钥、替换指纹或公开备份文件。
旧 `OKAVANGO_DEBUG_KEYSTORE_B64` 已弃用；发布只读取明确配置的固定密钥。

## 生成 APK 和 Release

普通代码 push 会执行测试、Lint 和源码编译；三项 Secrets 都具备时再生成可安装 APK。
未配置签名时会明确提示只完成源码验证，不会用随机签名生成一个无法持续升级的车测包。

Actions → **Okavango Android 5.1 checks and APK** → Run workflow，勾选 `create_release`。
通过验证后生成 `okavango-installable-release` artifact 和 GitHub **草稿 Release**。
草稿可供仓库有权限的成员下载；它不向公众开放，也不会出现在应用的公开更新检查中。

认证 APK 内含可提取的 MFi 材料；发布流程不会自动公开这些材料。
只有在材料的分发授权允许时，才手动公开 Release。公开后，应用才会通过本仓库更新渠道发现它。

每个新 Release 都先增加 `mobile/build.gradle.kts` 中的 `versionCode` 和 `versionName`。
版本格式为 `x.y.z`；Release 标签自动对应 `vx.y.z`。已有标签不覆盖、不删除。
APK 同时附带 `.sha256`、签名指纹和 `release.json`，验证脚本检查实际包名、API、ABI、V1 签名和证书。

本地源码验证（不需要 MFi）：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug
```

`assembleDebug` 只用于开发验证，不是可独立连接手机的分发包。
