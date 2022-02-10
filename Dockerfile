FROM golang:1.17-alpine as builder

ENV GO111MODULE=on \
    CGO_ENABLED=0 \
    GOOS=linux \
    GOARCH=amd64

WORKDIR /app

COPY ./server .

RUN go build -v .

FROM alpine:3.14

WORKDIR /app

EXPOSE 14444

COPY --from=builder /app/server ./server

RUN apk --no-cache add ca-certificates

ENTRYPOINT ["./server"]