package main

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
)

func main() {
	CreateDateDir("C:/user1")
	println("12345")

}

// CreateDateDir 根据当前日期来创建文件夹
func CreateDateDir(Path string) string {
	folderName := time.Now().Format("2006-01-02150404")
	fmt.Println(folderName)
	folderPath := filepath.Join(Path, folderName)
	if _, err := os.Stat(folderPath); os.IsNotExist(err) {
		// 必须分成两步：先创建文件夹、再修改权限
		os.Mkdir(folderPath, 0777) //0777也可以os.ModePerm
		os.Chmod(folderPath, 0777)
	}
	return folderPath
}
