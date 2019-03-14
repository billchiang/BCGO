package main

import (
	"fmt"
)

func main() {
	a, b := 1, 2
	a, b = swap(a, b)
	fmt.Println("a: ", a)
	fmt.Println("b: ", b)
	fmt.Println(Goroutine())
}

func swap(i, j int) (int, int) {
	return j, i
}
