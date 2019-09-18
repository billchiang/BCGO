package LdapAuth

import (
	"fmt"

	"github.com/billchiang/ldap-test-tool/g"
	"github.com/billchiang/ldap-test-tool/models"
)

func ParseConfig(cfg string) {
	g.ParseConfigJson(cfg)
}

func Single_Auth(username string, password string) (bool, error) {

	_, err := models.Single_Auth(g.Config().Ldap, username, password)

	if err != nil {
		fmt.Printf("%s auth failed: %s \n", username, err.Error())

		return false, err
	}
	fmt.Printf("%s auth successed \n", username)
	return true, err
}
