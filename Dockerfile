FROM golang:1.17 as builder

# 启用go module
ENV GO111MODULE=on \
    CGO_ENABLED=0 \
    GOOS=linux \
    GOARCH=amd64

WORKDIR /app

COPY . .

RUN go build -v .

FROM scratch

WORKDIR /app

EXPOSE 14444

COPY --from=builder /app/server ./server
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/cert

ENTRYPOINT ["./server"]