package main

import (
	"crypto/rand"
	"fmt"
	"github.com/casbin/casbin/v2"
	"github.com/gin-gonic/gin"
	"io"
	"io/ioutil"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
)

const APIURL = "https://api.xmpush.xiaomi.com/v2/message/user_account"

var enforcer, _ = casbin.NewEnforcer("model.conf", "policy.csv")

var n, _ = rand.Int(rand.Reader, big.NewInt(100))

var notifyID = n.Int64()

func main() {
	if os.Getenv("CI") == "" {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}

	if len(os.Args) <= 1 {
		fmt.Println("MiPush Secret not available")
		os.Exit(-1)
	}
	key := os.Args[1]
	authHeader := fmt.Sprintf("key=%s", key)

	router := gin.Default()

	router.GET("/:user_id/check", func(context *gin.Context) {
		userID := context.Param("user_id")
		result, _ := enforcer.Enforce(userID)
		context.String(http.StatusOK, strconv.FormatBool(result))
		return
	})

	router.GET("/:user_id/send", func(context *gin.Context) {
		userID := context.Param("user_id")
		result, _ := enforcer.Enforce(userID)
		if !result {
			context.String(http.StatusForbidden, "Unauthorized")
			return
		}

		title := context.DefaultQuery("title", "Notification")
		content := context.Query("content")
		long := context.Query("long")

		if content == "" {
			context.String(http.StatusBadRequest, "Content can not be empty.")
			return
		}
		intentData := url.Values{
			"title":   {title},
			"content": {content},
			"payload": {long},
		}.Encode()

		fmt.Println("mipush://view?" + intentData) // https://dev.mi.com/console/doc/detail?pId=1278#_3_2

		postData := url.Values{
			"user_account":            {userID},
			"payload":                 {long},
			"restricted_package_name": {"top.learningman.mipush"},
			"pass_through":            {"0"},
			"title":                   {title},
			"description":             {content},
			"notify_id":               {strconv.Itoa(int(notifyID))},
			"extra.notify_effect":     {"2"},
			"extra.intent_uri":        {"mipush://view?" + intentData},
		}.Encode()

		req, err := http.NewRequest(
			http.MethodPost,
			APIURL,
			strings.NewReader(postData))

		req.Header.Set("Authorization", authHeader)
		req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

		client := &http.Client{}
		resp, err := client.Do(req)

		body, _ := ioutil.ReadAll(resp.Body)
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

		statusCode := resp.StatusCode

		context.String(statusCode, bodyStr)
	})

	err := router.Run("0.0.0.0:14444")
	if err != nil {
		panic("ServerFailed")
	}
}
