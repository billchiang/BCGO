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

---
## 繁體中文 (台灣)

# Go 工具程式與範例

本專案是各種 Go 工具程式與範例的集合。每個子目錄都包含一個獨立的程式或模組。

歡迎在各個目錄中探索不同的工具程式與範例。

## 工具程式與範例

*   **Json**: 展示 Go 中的 JSON 編碼與解碼。
*   **LdapAuth**: 提供 LDAP 身份驗證功能。
*   **NTReConvertTool**: 一個用於重新轉換資料的工具（從程式碼來看細節有點不清楚，但涉及檔案路徑和輸出路徑）。
*   **PassParamToProcess**: Go 中的 RPC (遠端程序呼叫) 範例，包含伺服器和客戶端。
*   **WriteFileAndCreateDir**: 根據目前日期和時間建立目錄的工具程式。
*   **animals**: 定義 `Animal` 和 `Cat` 結構以及一個建立 `Cat` 實例的函式。
*   **defer**: Go 中使用 `defer` 陳述式的範例。
*   **dgraph**: 查詢 Dgraph 資料庫的範例。
*   **elapsed**: 用於測量和列印一段程式碼執行時間的工具函式。
*   **goroutine**: goroutine 的簡單示範。
*   **graphql**: 定義一個 GraphQL 結構描述，包含 "Goods" 的查詢和變更。
*   **regex**: Go 中使用正規表示式比對字串的範例。
*   **rpc_practice**: 展示 Go 中的 RPC，定義一個 `Arith` 服務，包含 `Multiply` 和 `Divide` 方法。
*   **sort**: 使用外部函式庫進行自然字串排序的範例。
*   **walk**: 遍歷目錄樹並列印檔案名稱的工具程式。

## 如何執行

這些工具程式與範例大多可以在其各自的子目錄中使用 Go 命令列工具執行。

例如，執行 `Json` 範例：
```bash
cd Json
go run json.go
```

對於包含 `main.go` 檔案的工具程式：
```bash
cd <directory_name>
go run main.go
```

某些範例或工具程式可能有特定的說明：
*   **LdapAuth**: 這是一個函式庫套件，旨在匯入到其他 Go 程式中。它沒有獨立的可執行檔。
*   **PassParamToProcess**: 此範例涉及 RPC 伺服器和客戶端。您需要執行 `server()` 函式（例如，`go run main.go`，此範例中會同時啟動伺服器和客戶端），或者如果它們位於不同的檔案中，則可能需要分別執行伺服器和客戶端元件。
*   **rpc_practice**: 此範例也涉及 RPC 伺服器和客戶端。首先執行伺服器：
    ```bash
    cd rpc_practice/rpc
    go run server.go
    ```
    然後，在另一個終端機中執行客戶端：
    ```bash
    cd rpc_practice/rpc
    go run client.go # 假設 client.go 存在且包含客戶端邏輯
    ```
    （注意：提供的 `rpc_practice/rpc/server.go` 似乎只有伺服器。需要一個對應的 `client.go` 才能完整展示，或者可以參考包含兩者的主要 `PassParamToProcess` 範例）。
*   **NTReConvertTool**: 此工具似乎需要命令列參數：`Name`、`FilePath` 和 `OutputPath`。例如：
    ```bash
    cd NTReConvertTool
    go run main.go "TestName" "input/file.txt" "output/result.txt"
    ```

如果需要，請參閱每個工具程式的原始程式碼以獲取更具體的詳細資訊。

## 貢獻

歡迎貢獻！如果您有新的 Go 工具程式或範例，或對現有的工具程式或範例進行了改進，請隨時：

1.  Fork 該儲存庫。
2.  為您的功能或錯誤修復建立一個新的分支。
3.  進行變更。
4.  使用清晰的提交訊息提交您的變更。
5.  將您的分支推送到您的 fork。
6.  向主儲存庫建立一個拉取請求。

請確保您的程式碼有良好的註釋，並且在適用的情況下，包含如何使用的範例。

## 授權條款

本專案採用 Apache License 2.0 授權。詳情請參閱 [LICENSE](LICENSE) 檔案。
