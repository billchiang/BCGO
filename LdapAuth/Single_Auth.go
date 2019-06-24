package LdapAuth

import (
	"fmt"

	"github.com/shanghai-edu/ldap-test-tool/g"
	"github.com/shanghai-edu/ldap-test-tool/models"
)

func init() {
	g.ParseConfig("cfg.json")
}

func Single_Auth(username string, password string) {

	_, err := models.Single_Auth(g.Config().Ldap, username, password)

	if err != nil {
		fmt.Printf("%s auth failed: %s \n", username, err.Error())

		return
	}
	fmt.Printf("%s auth successed \n", username)

}
