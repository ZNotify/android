package main

import (
	"fmt"
	"github.com/casbin/casbin/v2"
	"github.com/gin-gonic/gin"
	"io"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
)

const APIURL = "https://api.xmpush.xiaomi.com/v2/message/user_account"

var enforcer, _ = casbin.NewEnforcer("model.conf", "policy.csv")
var notifyID = 371102

func main() {
	gin.SetMode(gin.DebugMode)

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
		long := context.DefaultQuery("long", "")

		if content == "" {
			context.String(http.StatusBadRequest, "Content can not be empty.")
			return
		}

		req, err := http.NewRequest(
			http.MethodPost,
			APIURL,
			strings.NewReader(url.Values{
				"payload":                 {long},
				"restricted_package_name": {"top.learningman.mipush"},
				"pass_through":            {"0"},
				"title":                   {title},
				"description":             {content},
				"notify_id":               {strconv.Itoa(notifyID)},
				"user_account":            {userID},
			}.Encode()))

		req.Header.Set("Authorization", authHeader)

		client := &http.Client{}
		resp, err := client.Do(req)
		if err == nil {
			context.String(http.StatusInternalServerError, "Failed Request.\n%s",)
			return
		}
		defer func(Body io.ReadCloser) {
			err := Body.Close()
			if err != nil {
				panic("Failed to Close Connection")
			}
		}(resp.Body)

		statusCode := resp.StatusCode
		body, _ := ioutil.ReadAll(resp.Body)
		bodyStr := string(body)

		context.String(statusCode, bodyStr)
	})

	err := router.Run("0.0.0.0:14444")
	if err != nil {
		panic("ServerFailed")
	}
}