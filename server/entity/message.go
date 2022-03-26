package entity

import "time"

type Message struct {
	ID        string
	UserID    string
	Title     string
	Content   string
	Long      string
	CreatedAt time.Time
}
