package goroutine

import (
	"fmt"
	"testing"
	"time"
)

func TestSay(t *testing.T) {
	for i := 0; i < 5; i++ {
		time.Sleep(100 * time.Millisecond)
		fmt.Println("Hello")
	}
}
