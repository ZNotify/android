package push

import (
	"context"
	firebase "firebase.google.com/go"
	"github.com/Zxilly/Notify/server"
)

func SendViaFCM(app *firebase.App, msg *server.Message) error {
	// https://firebase.google.com/docs/cloud-messaging/send-message#example-notification-click-action
	ctx := context.Background()
	client, err := app.Messaging(ctx)
	if err != nil {
		return err
	}

}
