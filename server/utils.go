package server

import "github.com/gin-gonic/gin"

func breakOnError(c *gin.Context, err error) {
	if err != nil {
		e := c.AbortWithError(500, err)
		if e != nil {
			panic(e)
		}
	}
}
