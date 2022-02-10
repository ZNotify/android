FROM golang:1.17-alpine as builder

# 启用go module
ENV GO111MODULE=on \
    CGO_ENABLED=1 \
    GOOS=linux \
    GOARCH=amd64

WORKDIR /app

COPY ./server .

RUN go build -v .

FROM alpine:3.14

WORKDIR /app

EXPOSE 14444

COPY --from=builder /app/server ./server
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/cert

ENTRYPOINT ["./server"]