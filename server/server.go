package main

import (
	"bufio"
	"crypto/rand"
	"embed"
	"errors"
	"fmt"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/glebarez/sqlite"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"io"
	"io/fs"
	"io/ioutil"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
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

	if len(os.Args) <= 1 {
		fmt.Println("MiPush Secret not available")
		os.Exit(-1)
	}
	key := os.Args[1]
	authHeader := fmt.Sprintf("key=%s", key)

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
		n, _ := rand.Int(rand.Reader, big.NewInt(1000000))
		notifyID := n.Int64()

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

		intentUriFormat := "intent:#Intent;launchFlags=0x14000000;component=top.learningman.push/.TranslucentActivity;S.userID=%s;S.long=%s;S.msgID=%s;S.title=%s;S.createdAt=%s;S.content=%s;end"
		intentUri := fmt.Sprintf(intentUriFormat,
			url.QueryEscape(userID),
			url.QueryEscape(long),
			url.QueryEscape(msgID),
			url.QueryEscape(title),
			url.QueryEscape(time.Now().Format(time.RFC3339)),
			url.QueryEscape(content))

		postData := url.Values{
			"user_account":            {userID},
			"payload":                 {long},
			"restricted_package_name": {"top.learningman.push"},
			"pass_through":            {"0"},
			"title":                   {title},
			"description":             {content},
			"notify_id":               {strconv.Itoa(int(notifyID))},
			"extra.id":                {msgID},
			"extra.notify_effect":     {"2"}, // https://dev.mi.com/console/doc/detail?pId=1278#_3_2
			"extra.intent_uri":        {intentUri},
		}.Encode()

		req, err := http.NewRequest(
			http.MethodPost,
			APIURL,
			strings.NewReader(postData))

		req.Header.Set("Authorization", authHeader)
		req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

		client := &http.Client{}
		resp, err := client.Do(req)

		if err != nil {
			context.String(http.StatusInternalServerError, "Failed Request.\n%s", err)
			return
		}

		body, err := ioutil.ReadAll(resp.Body)

		if err != nil {
			context.String(http.StatusInternalServerError, "Failed Request.\n%s", err)
			return
		}

		bodyStr := string(body)

		if err != nil {
			context.String(http.StatusInternalServerError, "Failed Request.\n%s\n%s", bodyStr, err)
			return
		}
		defer func(Body io.ReadCloser) {
			err := Body.Close()
			if err != nil {
				panic("Failed to Close Connection")
			}
		}(resp.Body)

		// Insert message record
		db.Create(&Message{
			ID:      msgID,
			UserID:  userID,
			Title:   title,
			Content: content,
			Long:    long,
		})

		context.String(resp.StatusCode, bodyStr)
	})

	//func getFileSystem() http.FileSystem {
	//	fsys, err := fs.Sub(dist, "gui/dist")
	//	if err != nil {
	//	log.Fatal(err)
	//}
	//	return http.FS(fsys)
	//}

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
