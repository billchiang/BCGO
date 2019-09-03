// rpc
package main

import (
	"fmt"
	"net"
	"net/rpc"
	"runtime"
	"sync"
)

var wg sync.WaitGroup

type Server struct{}

func (this *Server) Add(u [2]int64, reply *int64) error {
	*reply = u[0] + u[1]
	return nil
}

func server() {
	fmt.Println("server: Hi")
	rpc.Register(new(Server))
	ln, err := net.Listen("tcp", "127.0.0.1:12345")
	if err != nil {
		fmt.Println(err)
		return
	}
	for {
		c, err := ln.Accept()
		if err != nil {
			continue
		}
		go rpc.ServeConn(c)
	}
}

func client() {
	wg.Add(1)
	c, err := rpc.Dial("tcp", "127.0.0.1:8866")
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Connected...")
	var result int64
	err = c.Call("Server.Add", [2]int64{10, 20}, &result)
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Server.Add(10,20) =", result)
	}
	wg.Done()
}

func main() {

	go client()
	runtime.Gosched()
	wg.Wait()
	fmt.Println("Bye")
}

/*output:
server: Hi
Connected...
Server.Add(10,20) = 30
Bye
*/
