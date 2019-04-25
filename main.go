package main

import (
	"fmt"
	"log"
	"reflect"

	"github.com/billchiang/BCGO/animals"
	"github.com/billchiang/BCGO/models"
	"github.com/billchiang/BCGO/g"
	"github.com/lithammer/fuzzysearch/fuzzy"
	"github.com/spf13/afero"
)

type userSetting struct {
	Scope string
}

type LdapAuth (username string, password string){

	startTime := time.Now()

	_, err := models.Single_Auth(g.Config().Ldap, username, password)

	if err != nil {
		fmt.Printf("%s auth test failed: %s \n", username, err.Error())
		return
	}
	fmt.Printf("%s auth test successed \n", username)
}

func main() {

	bill := animals.Getbill("meow")
	//print type of bill
	fmt.Println(reflect.TypeOf(bill))
	//fmt.Printf("%+v\n", bill.Animal)

}

//模糊搜尋練習
func fuzzysearchMatch(s string) {
	target := []string{"cartwheel.txt", "foobar.ccc", "carwheel.txt", "baz"}
	matches := fuzzy.Find(s, target)
	fmt.Print(matches)
}

//閉包練習
// func PrintInputString(s string, found func(path string) string) string {
// 	InputString := s
// 	return found(InputString)
// }

//CreateFile a using afero
func CreateFile() {
	s := &userSetting{
		Scope: "C:/",
	}

	fs := afero.NewBasePathFs(afero.NewOsFs(), s.Scope)
	//delete a file
	// fs := afero.NewOsFs()
	_, err := fs.Create("/file.html")
	if err != nil {
		log.Print("1")
	}

}
