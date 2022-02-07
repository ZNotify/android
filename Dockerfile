FROM scratch

WORKDIR /app

EXPOSE 14444

COPY server/server /app/server

ENTRYPOINT ["/app/server"]