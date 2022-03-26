package push

import (
	"context"
	"firebase.google.com/go/v4/messaging"
	"github.com/Zxilly/Notify/server/entity"
	"time"
)

func SendViaFCM(client *messaging.Client, registrationIDs []string, msg *entity.Message) error {
	if len(registrationIDs) == 0 {
		return nil
	}

	// https://firebase.google.com/docs/cloud-messaging/send-message#example-notification-click-action
	fcmMsg := messaging.MulticastMessage{
		Notification: &messaging.Notification{
			Title: msg.Title,
			Body:  msg.Content,
		},
		Data: map[string]string{
			"userID":    msg.UserID,
			"long":      msg.Long,
			"msgID":     msg.ID,
			"title":     msg.Title,
			"content":   msg.Content,
			"createdAt": msg.CreatedAt.Format(time.RFC3339),
		},
		Android: &messaging.AndroidConfig{
			Notification: &messaging.AndroidNotification{
				ClickAction: "TranslucentActivity",
			},
		},
		Tokens: registrationIDs,
	}
	_, err := client.SendMulticast(context.Background(), &fcmMsg)
	if err != nil {
		return err
	}
	return nil
}
