package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"net/http"
	"os"
)

func breakOnError(c *gin.Context, err error) {
	if err != nil {
		e := c.AbortWithError(500, err)
		if e != nil {
			panic(e)
		}
	}
}

// Check internet connection to google firebase
func checkInternetConnection() {
	_, err := http.Get("https://www.google.com/robots.txt")
	if err != nil {
		fmt.Println("No internet connection")
		fmt.Printf("%s", err)
		os.Exit(1)
	}
}
