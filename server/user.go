package main

import (
	"bufio"
	"fmt"
	"os"
)

// read file users.txt to get user list
func readUsers() []string {
	file, err := os.Open("users.txt")
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
	}(file)

	var users []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		users = append(users, scanner.Text())
	}

	if err := scanner.Err(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	return users
}

// judge user if in the user list
func isUser(user string, users []string) bool {
	for _, u := range users {
		if u == user {
			return true
		}
	}
	return false
}
