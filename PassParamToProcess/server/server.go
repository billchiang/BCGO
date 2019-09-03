package main

import (
	"net/http"
	"log"
	"flag"
	"sync"
)

var (
	i,p string
	m sync.Mutex
)

func init(){
	flag.StringVar(&i , "i", "", "set ip")
	flag.StringVar(&p , "p", "8866", "set listen port")
}

func main() {
	flag.Parse()

	http.HandleFunc("/", handler)
	
	l := i +":"+ p;
	log.Println("listening at: ",l)
	http.ListenAndServe(l, nil)
	
}

func handler(w http.ResponseWriter, r *http.Request) {
	message := "Request URL Path is " + r.URL.Path
	m.Lock()
	m.Unlock()
	w.Write([]byte(message))
}
