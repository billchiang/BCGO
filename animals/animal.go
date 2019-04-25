package animals

import (
	"fmt"
)

//
type Animal struct {
	Name           string
	Height, Weight int
}

//
type Cat struct {
	Animal
	Color string
}

func Getbill(s string) Cat {
	a := Animal{"bill", 168, 74}
	bill := Cat{

		Color: "red",
	}
	bill.Animal = a
	fmt.Printf("%s say: %s\n", bill.Name, s)
	return bill
}
