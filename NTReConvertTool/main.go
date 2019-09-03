package main

import (
	"fmt"
	"log"
	"os"
	"strings"
)

//ReConvertInfo is Rawdata to reconvert info
type ReConvertInfo struct {
	Name       string `json:"name"`
	FilePath   string `json:"filepath"`
	OutputPath string `json:"outputpath"`
}

func main() {
	if len(os.Args) < 5 {
		i := ReConvertInfo{Name: os.Args[1], FilePath: os.Args[2], OutputPath: os.Args[3]}
		fmt.Println(i.Name, i.FilePath, i.OutputPath)

		r := strings.NewReader(i.FilePath)
		o := strings.NewReader(i.OutputPath)
		c := strings
		destination, err := os.Create("bar.txt")
		if err != nil {
			log.Fatal(err)
		}
		defer destination.Close()

		os.Exit(0)
	}
	os.Exit(1)
	// if i.Name == "" || i.FilePath == "" || i.OutputPath == "" {
	//
	// }
}
