package main

import (
	"log"
	"os"
	"os/exec"
)

func main() {
	cmd := exec.Command(`cmd.exe`, `/C`, os.Args[1])
	if cmd != nil {
		log.Fatal("error")
	}
	log.Fatal("ok")
}
