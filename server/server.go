package main

import (
	"context"
	"embed"
	"errors"
	firebase "firebase.google.com/go/v4"
	"fmt"
	"github.com/Zxilly/Notify/server/entity"
	"github.com/Zxilly/Notify/server/push"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/glebarez/sqlite"
	"github.com/google/uuid"
	"google.golang.org/api/option"
	"gorm.io/gorm"
	"io/fs"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"time"
)

//go:embed static/*
var f embed.FS

func main() {
	var err error

	if len(os.Args) <= 1 {
		fmt.Println("MiPush Secret not available")
		os.Exit(-1)
	}
	key := os.Args[1]
	miPushAuthHeader := fmt.Sprintf("key=%s", key)

	// check fcm credentials
	_, err = os.Stat("notify.json")
	if err != nil {
		fmt.Println("notify.json not found")
		os.Exit(1)
	}

	go checkInternetConnection()

	opt := option.WithCredentialsFile("notify.json")
	app, err := firebase.NewApp(context.Background(), nil, opt)
	if err != nil {
		fmt.Println(fmt.Errorf("error initializing app: %v", err))
		os.Exit(1)
	}
	fcmClient, err := app.Messaging(context.Background())
	if err != nil {
		fmt.Println(fmt.Errorf("error initializing app: %v", err))
		os.Exit(1)
	}
	httpClient := &http.Client{}

	pureFs, err := fs.Sub(f, "static")
	if err != nil {
		panic(err)
	}

	users := readUsers()

	// Determining whether notify.db is directory
	va, err := os.Stat("notify.db")
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	if va.IsDir() {
		fmt.Println("notify.db is directory.")
		os.Exit(1)
	}

	db, err := gorm.Open(sqlite.Open("notify.db"), &gorm.Config{})
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	err = db.AutoMigrate(&entity.Message{}, &entity.Tokens{})
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	router := gin.Default()
	router.Use(cors.Default())

	router.GET("/:user_id/check", func(context *gin.Context) {
		userID := context.Param("user_id")
		result := isUser(userID, users)
		context.String(http.StatusOK, strconv.FormatBool(result))
		return
	})

	router.PUT("/:user_id/token", func(context *gin.Context) {
		userID := context.Param("user_id")
		token, err := ioutil.ReadAll(context.Request.Body)
		if err != nil {
			context.String(http.StatusBadRequest, err.Error())
			return
		}
		tokenString := string(token)
		result := isUser(userID, users)
		if !result {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}

		var cnt int64
		db.Model(&entity.Tokens{}).
			Where("user_id = ?", userID).
			Where("registration_id = ?", tokenString).
			Count(&cnt)
		// TODO: update user with same token
		if cnt > 0 {
			context.String(http.StatusOK, "Token already exists")
		} else {
			user := entity.Tokens{
				ID:             uuid.New().String(),
				UserID:         userID,
				RegistrationID: tokenString,
			}
			db.Create(&user)
			context.String(http.StatusOK, "Registration ID saved.")
		}
	})

	// return message in 30 days
	router.GET("/:user_id/record", func(context *gin.Context) {
		userID := context.Param("user_id")
		auth := isUser(userID, users)
		if !auth {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}
		var messages []entity.Message
		result := db.Where("user_id = ?", userID).
			Where("created_at > ?", time.Now().AddDate(0, 0, -30)).
			Order("created_at desc").
			Find(&messages)
		breakOnError(context, result.Error)

		var ret []gin.H
		for i := range messages {
			ret = append(ret, gin.H{
				"id":        messages[i].ID,
				"title":     messages[i].Title,
				"content":   messages[i].Content,
				"long":      messages[i].Long,
				"createdAt": messages[i].CreatedAt.Format(time.RFC3339),
			})
		}
		context.JSON(http.StatusOK, ret)
	})

	// delete message
	router.DELETE("/:user_id/:id", func(context *gin.Context) {
		userID := context.Param("user_id")
		id := context.Param("id")

		auth := isUser(userID, users)
		if !auth {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}

		var message entity.Message
		result := db.Where("user_id = ?", userID).
			Where("id = ?", id).
			First(&message)
		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			context.String(http.StatusNotFound, "Not Found")
			return
		} else {
			breakOnError(context, err)
		}
		result = db.Delete(&message)
		breakOnError(context, err)

		context.String(http.StatusOK, "OK")
		return
	})

	router.POST("/:user_id/send", func(context *gin.Context) {
		userID := context.Param("user_id")
		result := isUser(userID, users)
		if !result {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}

		// Build MIPush request
		title := context.DefaultPostForm("title", "Notification")
		content := context.PostForm("content")
		long := context.PostForm("long")

		if content == "" {
			context.String(http.StatusBadRequest, "Content can not be empty.")
			return
		}
		msgID := uuid.New().String()

		message := &entity.Message{
			ID:        msgID,
			UserID:    userID,
			Title:     title,
			Content:   content,
			Long:      long,
			CreatedAt: time.Now(),
		}

		var tokens []entity.Tokens
		dbResult := db.Where("user_id = ?", userID).Find(&tokens)
		breakOnError(context, dbResult.Error)

		var registrationIDs []string
		for i := range tokens {
			registrationIDs = append(registrationIDs, tokens[i].RegistrationID)
		}

		err := push.SendViaMiPush(httpClient, miPushAuthHeader, message)
		if err != nil {
			context.String(http.StatusInternalServerError, fmt.Sprintf("%s", err))
		}

		err = push.SendViaFCM(fcmClient, registrationIDs, message)
		if err != nil {
			context.String(http.StatusInternalServerError, fmt.Sprintf("%s", err))
		}

		// Insert message record
		db.Create(message)

		context.String(200, fmt.Sprintf("message %s sent to %s.", msgID, userID))
	})

	router.StaticFS("/fs", http.FS(pureFs))

	router.GET("/", func(context *gin.Context) {
		context.FileFromFS("/", http.FS(pureFs))
		// hardcode index.html, use this as a trick to get html file
		// https://github.com/golang/go/blob/a7e16abb22f1b249d2691b32a5d20206282898f2/src/net/http/fs.go#L587
	})

	err = router.Run("0.0.0.0:14444")
	if err != nil {
		panic("ServerFailed")
	}
}
