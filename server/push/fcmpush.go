package push

import (
	"firebase.google.com/go/v4/messaging"
	"github.com/Zxilly/Notify/server/entity"
)

func SendViaFCM(client *messaging.Client, registrationIDs []string, msg *entity.Message) error {
	// https://firebase.google.com/docs/cloud-messaging/send-message#example-notification-click-action
	fcmMsg := messaging.Message{}
	_ = fcmMsg
	return nil
}
