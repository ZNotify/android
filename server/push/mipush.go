package push

import (
	"crypto/rand"
	"fmt"
	"github.com/Zxilly/Notify/server/entity"
	"io"
	"io/ioutil"
	"math/big"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

const APIURL = "https://api.xmpush.xiaomi.com/v2/message/user_account"

func SendViaMiPush(client *http.Client, authHeader string, msg *entity.Message) error {
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
		url.QueryEscape(msg.CreatedAt.Format(time.RFC3339)),
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

	resp, err := client.Do(req)

	if err != nil {
		return err
	}

	body, err := ioutil.ReadAll(resp.Body)

	fmt.Printf("%s", body)

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
