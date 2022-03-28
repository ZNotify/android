# Notify Server

Notify 服务端。

## 启动参数

```shell
server MIPUSH_TOKEN // MiPush 推送的 token
```

需要在同一目录下放置 FCM 的凭据文件，名称为 `notify.json`。

数据库文件名称为 `notify.db`, `sqlite3` 格式。

在 `Docker` 中启动服务，如果想将数据库映射到宿主机，需要预先在宿主机上 `touch notify.db`, 并使用 `-v` 映射。

## 请求参数
```
POST https://push.learningman.top/{user_id}/send

@path  user_id 用户 ID
@param title   推送标题
@param content 推送内容
@param long    传送到客户端的长内容, 需要点击查看
```

`long` 支持 markdown 格式， 支持使用表格扩展。

## 构建
构建前端后，执行
```shell
go build -v github.com/Zxilly/Notify/server
```

## 前端
请查看 `server/frontend/README.md`