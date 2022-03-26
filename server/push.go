package main

import (
	"crypto/rand"
	"fmt"
	"io"
	"io/ioutil"
	"math/big"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

func MiPush(authHeader string, msg *Message) error {
	n, _ := rand.Int(rand.Reader, big.NewInt(1000000))
	notifyID := n.Int64()

	// Build MIPush request
	title := msg.Title
	content := msg.Content
	long := msg.Long

	msgID := msg.ID

	intentUriFormat := "intent:#Intent;launchFlags=0x14000000;component=top.learningman.push/.TranslucentActivity;S.userID=%s;S.long=%s;S.msgID=%s;S.title=%s;S.createdAt=%s;S.content=%s;end"
	intentUri := fmt.Sprintf(intentUriFormat,
		url.QueryEscape(msg.UserID),
		url.QueryEscape(long),
		url.QueryEscape(msgID),
		url.QueryEscape(title),
		url.QueryEscape(time.Now().Format(time.RFC3339)),
		url.QueryEscape(content))

	postData := url.Values{
		"user_account":            {msg.UserID},
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
		return err
	}

	_, err = ioutil.ReadAll(resp.Body)

	if err != nil {
		return err
	}

	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			panic("Failed to Close Connection")
		}
	}(resp.Body)

	return nil
}
