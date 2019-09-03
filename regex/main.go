package main

import (
	"regexp"
)

func main() {
	r, _ := regexp.MatchString(`^[CTCB]{4}$`, `D:\CTCB`)

	println(r)
}
