# Notify

通过各移动设备的推送服务，接收通知。

当前支持的推送服务有：

- 【MiPush】：小米推送
- 【FCM】：Google Firebase Cloud Messaging

> 当系统无原生推送框架支持时，将使用 MiPush 的独立版本作为后备。

联系 @Zxilly 获取 `https://push.learningman.top` 使用权限。

查看 [server](https://github.com/ZNotify/server) 了解服务端。

## Download

[AppCenter](https://install.appcenter.ms/users/zxilly/apps/notify/distribution_groups/public)

[Github Release](https://github.com/ZNotify/android/releases)

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