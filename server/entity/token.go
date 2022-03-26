package entity

import "time"

type Tokens struct {
	ID             string
	UserID         string
	CreatedAt      time.Time
	RegistrationID string
}
