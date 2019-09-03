type School struct {
    Name string `json:"name,omitempty"`
}

type loc struct {
    Type   string    `json:"type,omitempty"`
    Coords []float64 `json:"coordinates,omitempty"`
}

// If omitempty is not set, then edges with empty values (0 for int/float, "" for string, false
// for bool) would be created for values not specified explicitly.

type Person struct {
    Uid      string   `json:"uid,omitempty"`
    Name     string   `json:"name,omitempty"`
    Age      int      `json:"age,omitempty"`
    Married  bool     `json:"married,omitempty"`
    Raw      []byte   `json:"raw_bytes,omitempty"`
    Friends  []Person `json:"friend,omitempty"`
    Location loc      `json:"loc,omitempty"`
    School   []School `json:"school,omitempty"`
}

dg, cancel := getDgraphClient()
defer cancel()
// While setting an object if a struct has a Uid then its properties in the graph are updated
// else a new node is created.
// In the example below new nodes for Alice, Bob and Charlie and school are created (since they
// don't have a Uid).
p := Person{
    Name:    "Alice",
    Age:     26,
    Married: true,
    Location: loc{
        Type:   "Point",
        Coords: []float64{1.1, 2},
    },
    Raw: []byte("raw_bytes"),
    Friends: []Person{{
        Name: "Bob",
        Age:  24,
    }, {
        Name: "Charlie",
        Age:  29,
    }},
    School: []School{{
        Name: "Crown Public School",
    }},
}

op := &api.Operation{}
op.Schema = `
		age: int .
		married: bool .
	`

ctx := context.Background()
if err := dg.Alter(ctx, op); err != nil {
    log.Fatal(err)
}

mu := &api.Mutation{
    CommitNow: true,
}
pb, err := json.Marshal(p)
if err != nil {
    log.Fatal(err)
}

mu.SetJson = pb
assigned, err := dg.NewTxn().Mutate(ctx, mu)
if err != nil {
    log.Fatal(err)
}

// Assigned uids for nodes which were created would be returned in the assigned.Uids map.
puid := assigned.Uids["blank-0"]
const q = `query Me($id: string){
		me(func: uid($id)) {
			name
			age
			loc
			raw_bytes
			married
			friend @filter(eq(name, "Bob")) {
				name
				age
			}
			school {
				name
			}
		}
	}`

variables := make(map[string]string)
variables["$id"] = puid
resp, err := dg.NewTxn().QueryWithVars(ctx, q, variables)
if err != nil {
    log.Fatal(err)
}

type Root struct {
    Me []Person `json:"me"`
}

var r Root
err = json.Unmarshal(resp.Json, &r)
if err != nil {
    log.Fatal(err)
}

// R.Me would be same as the person that we set above.
// fmt.Printf("Me: %+v\n", r.Me)

fmt.Println(string(resp.Json))