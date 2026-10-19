# Okavango 签名与发布

包名固定为 `com.shihab.diplay.okavango`，最低 Android 5.1 / API 22。
正式 APK 使用不可调试的 `release` 构建类型，启用 V1 + V2 签名。
`signing-cert.sha256` 固定本项目证书指纹；签名私钥不会装进 APK。

## 一次配置

使用新版 `Okavango-Signing-Secure-Backup.zip`。它对原来同一把私钥重新加密，证书与签名身份不变。
旧的弱口令备份不再用于新构建。新版备份含密钥及密码，必须私下保存，不得上传到公开仓库。

1. 仓库 **Settings → Environments → New environment**，名称填 `okavango-release`。
2. 在该环境的 **Deployment branches and tags** 中选 **Selected branches and tags**，新增 **Branch: main**。
   不要添加通配符或 Tag 规则。无需额外配置审批人；仓库写权限用户手动触发发布。
3. 在该环境的 **Environment secrets** 中添加两项，分别粘贴备份内同名 TXT 的完整内容：

   | Secret | 内容 |
   | --- | --- |
   | `OKAVANGO_KEYSTORE_B64` | 新版备份中同名 TXT；重新加密后的签名文件 |
   | `OKAVANGO_KEYSTORE_PASSWORD` | 新版备份中同名 TXT；随机生成的强密码 |

4. **Settings → Secrets and variables → Actions → Repository secrets** 中，原有
   `DIPLAY_MFI_PK8_B64` 和 `DIPLAY_MFI_P7B_B64` 保持原值。
   若存在旧的 `OKAVANGO_KEYSTORE_B64`、`OKAVANGO_KEYSTORE_PASSWORD` 或
   `OKAVANGO_DEBUG_KEYSTORE_B64` 仓库级 Secret，删除这些旧签名项；签名只放在上述发布环境。

MFi 两项是既有认证输入，不是 APK 签名。仓库写权限用户可以修改工作流并访问仓库级 Secrets，
因此只给受信任人员写权限。需要进一步隔离时，可把 MFi 两项也迁入同一个 Environment，值保持不变。

Gradle 从环境变量读取密码，不在命令行或源码中保存。PKCS12 的 store/key 使用同一个强密码。
密钥 alias 仍为 `androiddebugkey`，仅为条目名称；它不是 Android 公用签名，也不使 APK 变成 debug 包。
默认 `android` 口令已禁用。只允许显式密码和固定证书，错误输入会在打包前失败。

## 日常检查与发布

- 代码 push、PR：只做公共源码扫描、单元测试、Lint 和源码编译，不读取签名或 MFi Secrets。
- 正式发布：**Actions → Okavango Android 5.1 checks and APK → Run workflow**，选择 **main**，
  保持 `create_release` 勾选。检查通过后，独立 runner 在 `okavango-release` 环境中打包。
- 非 main 分支的发布请求会失败；环境的 main 分支规则提供另一层服务器端限制。
- 签名阶段不恢复或写入 Gradle 缓存，禁用任务/配置缓存；结束时清理认证材料和构建输出。
- 实际 APK 校验包名、API22、双 ARM ABI、不可调试属性、V1 签名、固定证书及认证资产。

成功后到仓库 **Releases** 页面，以拥有仓库写权限的账号查看草稿及下载 APK。
APK、SHA-256 校验文件、证书指纹和 `release.json` 直接上传到草稿，不作为 Actions artifact 分发。
公开仓库的 Actions artifacts 可被具有读权限的已登录 GitHub 用户下载；不能将其当作私有存储。
只有无私密材料的测试报告保留为 Actions artifact。

迁移工作流把旧的 `okavango-mfi-test` / `okavango-installable-release` 附件转存到
`legacy-car-test-backup` 草稿，回下载并逐字节校验后才删除公开原件。旧 Gradle 缓存也会清理。
此操作无法撤回已经被第三方下载的副本。历史测试包保留原签名，不用于新的固定签名发布。

流程不会自动点击 Publish。认证 APK 内含可提取的 MFi 私钥；Secrets 保护构建输入，
无法保护已经交到接收者手里的 APK。公开 Release 前需确认认证材料允许该分发范围。
草稿不是加密容器；有仓库写权限的成员可访问。

## 后续升级

每次新 Release 增加 `mobile/build.gradle.kts` 中的 `versionCode` 和 `versionName`。
版本格式 `x.y.z`；Release 标签自动对应 `vx.y.z`。已有草稿、Release 或 Tag 不覆盖。
失败时若已留下草稿，检查后处理该草稿，或增加版本号重新发布，不反复覆盖已有版本。

此前随机 debug 签名包可能需要首次卸载；卸载会清除设置和配对。
后续同包名、同证书且更高版本的正式 APK 可覆盖安装。更改密钥库密码不会更改证书，
但重新生成私钥或证书会破坏 Android 5.1 的这条升级链。

本地源码验证（不需要私密材料）：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug
```

`assembleDebug` 仅用于开发验证，不是独立连接手机的分发包。
