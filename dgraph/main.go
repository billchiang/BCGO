package main

import (
	"context"
	"flag"
	"fmt"
	"log"

	"github.com/dgraph-io/dgo"
	"github.com/dgraph-io/dgo/protos/api"
	"google.golang.org/grpc"
)

var (
	dgraph = flag.String("d", "127.0.0.1:9080", "Dgraph Alpha address")
)

func main() {
	flag.Parse()
	conn, err := grpc.Dial(*dgraph, grpc.WithInsecure())
	if err != nil {
		log.Fatal(err)
	}
	defer conn.Close()

	dg := dgo.NewDgraphClient(api.NewDgraphClient(conn))

	resp, err := dg.NewTxn().Query(context.Background(), `{
  result(func: has(age), first: 10) {
    uid
    expand(_all_)
  }
}`)

	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Response: %s\n", resp.Json)
}
