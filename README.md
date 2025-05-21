# Go Utilities and Examples

This project is a collection of various Go utilities and examples. Each subdirectory contains a separate program or module.

Feel free to explore the different utilities and examples in their respective directories.

## Utilities and Examples

*   **Json**: Demonstrates JSON encoding and decoding in Go.
*   **LdapAuth**: Provides a function for LDAP authentication.
*   **NTReConvertTool**: A tool for reconverting data (details are a bit unclear from the code, but it involves file paths and output paths).
*   **PassParamToProcess**: Example of RPC (Remote Procedure Call) in Go, with a server and client.
*   **WriteFileAndCreateDir**: Utility to create a directory based on the current date and time.
*   **animals**: Defines `Animal` and `Cat` structs and a function to create a `Cat` instance.
*   **defer**: Example of using the `defer` statement in Go.
*   **dgraph**: Example of querying a Dgraph database.
*   **elapsed**: Utility function to measure and print the execution time of a piece of code.
*   **goroutine**: Simple demonstration of goroutines.
*   **graphql**: Defines a GraphQL schema with queries and mutations for "Goods".
*   **regex**: Example of using regular expressions in Go to match a string.
*   **rpc_practice**: Demonstrates RPC in Go, defining an `Arith` service with `Multiply` and `Divide` methods.
*   **sort**: Example of natural string sorting using an external library.
*   **walk**: Utility to walk a directory tree and print file names.

## How to Run

Most of these utilities and examples can be run from within their respective subdirectories using the Go command-line tool.

For example, to run the `Json` example:
```bash
cd Json
go run json.go
```

For utilities with a `main.go` file:
```bash
cd <directory_name>
go run main.go
```

Some examples or utilities may have specific instructions:
*   **LdapAuth**: This is a library package and is intended to be imported into other Go programs. It doesn't have a standalone executable.
*   **PassParamToProcess**: This example involves an RPC server and client. You'll need to run the `server()` function (e.g., `go run main.go` which starts both server and client in the example) or potentially run server and client components separately if they were in different files.
*   **rpc_practice**: This example also involves an RPC server and client. Run the server first:
    ```bash
    cd rpc_practice/rpc
    go run server.go
    ```
    Then, in a separate terminal, run the client:
    ```bash
    cd rpc_practice/rpc
    go run client.go # Assuming client.go exists and contains the client logic
    ```
    (Note: The provided `rpc_practice/rpc/server.go` seems to be a server only. A corresponding `client.go` would be needed to fully demonstrate it, or the main `PassParamToProcess` example which includes both could be referred to).
*   **NTReConvertTool**: This tool seems to expect command-line arguments: `Name`, `FilePath`, and `OutputPath`. For example:
    ```bash
    cd NTReConvertTool
    go run main.go "TestName" "input/file.txt" "output/result.txt"
    ```

Please refer to the source code of each utility for more specific details if needed.

## Contributing

Contributions are welcome! If you have a new Go utility or example, or an improvement to an existing one, please feel free to:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Make your changes.
4.  Commit your changes with a clear commit message.
5.  Push your branch to your fork.
6.  Create a pull request to the main repository.

Please ensure your code is well-commented and, if applicable, includes examples of how to use it.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
