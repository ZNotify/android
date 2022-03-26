package main

import (
	"bufio"
	"embed"
	"errors"
	"fmt"
	"github.com/XMLHexagram/emp"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/glebarez/sqlite"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"io/fs"
	"net/http"
	"os"
	"strconv"
	"time"
)

//go:embed static/*
var f embed.FS

const APIURL = "https://api.xmpush.xiaomi.com/v2/message/user_account"

// read file users.txt to get user list
func readUsers() []string {
	file, err := os.Open("users.txt")
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
	}(file)

	var users []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		users = append(users, scanner.Text())
	}

	if err := scanner.Err(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	return users
}

// judge user if in the user list
func isUser(user string, users []string) bool {
	for _, u := range users {
		if u == user {
			return true
		}
	}
	return false
}

func main() {
	var err error

	//if len(os.Args) <= 1 {
	//	fmt.Println("MiPush Secret not available")
	//	os.Exit(-1)
	//}
	//key := os.Args[1]

	type Tokens struct {
		MiToken  string
		FCMToken string
	}

	tokens := new(Tokens)
	err = emp.Parse(tokens)
	if err != nil {
		panic(err)
	}

	miPushAuthHeader := fmt.Sprintf("key=%s", tokens.MiToken)

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
	err = db.AutoMigrate(&Message{})
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

	// return message in 30 days
	router.GET("/:user_id/record", func(context *gin.Context) {
		userID := context.Param("user_id")
		auth := isUser(userID, users)
		if !auth {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}
		var messages []Message
		result := db.Where("user_id = ?", userID).
			Where("created_at > ?", time.Now().AddDate(0, 0, -30)).
			Order("created_at desc").
			Find(&messages)
		breakOnError(context, result.Error)

		var ret []interface{}
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

		var message Message
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

		message := &Message{
			ID:      msgID,
			UserID:  userID,
			Title:   title,
			Content: content,
			Long:    long,
		}

		//TODO: send message
		err := MiPush(miPushAuthHeader, message)
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
