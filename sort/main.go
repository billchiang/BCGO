package main

import (
	"strings"

	"github.com/maruel/natural"
)

func main() {
	println(natural.Less("abc", "ABC"))
	println(natural.Less("ABC", "abc"))
	println(strings.EqualFold("ABC", "abc"))
}
