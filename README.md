# Notify

通过各移动设备的推送服务，接收通知。

当前支持的推送渠道有：

- 【FCM】：Google Firebase Cloud Messaging
- 【WebSocket】：WebSocket 长连接

联系 @Zxilly 获取 `https://push.learningman.top` 使用权限。

查看 [server](https://github.com/ZNotify/server) 了解服务端。

## Download

[AppCenter](https://install.appcenter.ms/users/zxilly/apps/notify/distribution_groups/public)

[Github Release](https://github.com/ZNotify/android/releases)

<a href="https://play.google.com/store/apps/details?id=top.learningman.push"><img src="/static/google-play-badge.png" width="250"></a>

**提示：**

`app-free-release.apk` 不包含应用内更新。

`app-github-release.apk` 包含从 `Github` 下载应用内更新的代码。

`AppCenter` 的应用内更新由 `AppCenter` 提供。

`Google Play` 的应用内更新由 `Google Play In-app` 提供。

## TODO

- 支持推送到指定设备
- 支持推送到指定渠道
- 支持设备端选择推送渠道

## Self-hosting

通常，你应该修改

- `app/build.gradle` > `defaultConfig.applicationId`
- `app/src/main/kotlin/top/learningman/push/Constant.kt`
- `app/google-services.json`

## License

Distribute under BSD 3-Clause License