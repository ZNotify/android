# Notify

`ServerChan` 的 MiPush 渠道替代品

联系 @Zxilly 获取 `https://push.learningman.top` 使用权限。

## 参数
```
POST https://push.learningman.top/{user_id}/send

@param title 推送标题
@param content 推送内容
@param long 传送到客户端的长内容, 需要在应用内查看，不支持从通知启动
https://developer.android.com/guide/components/activities/background-starts
```

## Known Issue

在 MIUI 上，接收到的通知不会被存储
