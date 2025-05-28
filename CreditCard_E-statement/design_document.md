# Core AFP Generation Engine - Design Document

## 1. Proposed Architecture

*   **Architectural Pattern:** A microservices-based architecture is proposed. This will allow for scalability, flexibility, and independent development and deployment of different components of the system.
*   **Primary Services/Components:**
    *   **Template Management Service:**
        *   **Description:** Responsible for storing, retrieving, and managing AFP templates. This includes handling template versions and metadata. It will expose APIs for creating, updating, deleting, and listing templates.
        *   **Technology Suggestion:** Java Spring Boot or .NET Core Web API.
    *   **Data Mapping Service:**
        *   **Description:** Handles the ingestion of data from various sources (JSON, XML, Databases) and maps this data to the placeholders defined in the templates. It will also perform basic data transformations.
        *   **Technology Suggestion:** Java Spring Boot or .NET Core, potentially using libraries like Apache Camel for data transformation and integration.
    *   **AFP Generation Service:**
        *   **Description:** Takes a template (with mapped data) and generates the final AFP document. This service will encapsulate the chosen AFP generation library/framework. It will also handle resource embedding (fonts, images).
        *   **Technology Suggestion:** Java application utilizing Apache FOP.
    *   **Resource Management Service:**
        *   **Description:** Manages shared resources like fonts, images, and other assets required for AFP generation. This ensures resources are stored efficiently and are accessible to the AFP Generation Service.
        *   **Technology Suggestion:** Could be a dedicated service or integrated within the Template Management Service. Storage could leverage cloud solutions (like AWS S3, Azure Blob Storage) or a local file system/database.
    *   **API Gateway:**
        *   **Description:** Single entry point for all client requests. It will route requests to the appropriate backend services, handle authentication, and potentially perform some request/response transformations.
        *   **Technology Suggestion:** Spring Cloud Gateway, Netflix Zuul, or a cloud provider's API Gateway solution.
*   **Inter-service Communication Methods:**
    *   **Synchronous:** REST APIs (HTTP/HTTPS) will be the primary method for request/response interactions between services (e.g., API Gateway to Template Management, Data Mapping to Template Management).
    *   **Asynchronous:** Message Queues (e.g., RabbitMQ, Apache Kafka, AWS SQS, Azure Service Bus) can be used for decoupling services and handling long-running processes, such as the actual AFP generation request. For example, the API Gateway could place a generation request on a queue, and the AFP Generation Service would pick it up. This improves resilience and scalability.

## 2. AFP Generation Technologies

*   **Recommended Library/Framework:** **Apache FOP (Formatting Objects Processor)**
    *   **Reasoning:**
        *   **Java-based:** Directly integrable with Java applications, which is a core requirement.
        *   **AFP Output:** Explicitly supports AFP as an output format. The documentation indicates compliance with various standards, and while MO:DCA IS/3 specifics need further verification during implementation, FOP's extensible nature allows for customization if needed.
        *   **XSL-FO Driven:** FOP uses XSL-FO (Extensible Stylesheet Language Formatting Objects) as its input. XSL-FO is a powerful W3C standard for describing page layouts and content, allowing for complex document structures.
        *   **Open-Source:** Being open-source (Apache License 2.0) eliminates licensing costs and provides flexibility.
        *   **Mature and Well-Documented:** FOP is a mature project with a considerable amount of documentation and a community, which aids in development and troubleshooting.
        *   **Multiple Output Formats:** While AFP is the target, FOP's ability to output to PDF, PS, etc., can be beneficial for testing or alternative use cases.
*   **Alternative (Commercial - for consideration if FOP has limitations):**
    *   **IBM Document Composition Facility (DCF) or similar AFP utilities from major print vendors (e.g., Ricoh, Xerox).**
    *   **Reasoning:** These are industry-standard tools specifically designed for robust AFP creation and would guarantee MO:DCA IS/3 compliance. However, they come with licensing costs and potentially more complex integration. This would be a fallback if open-source options prove insufficient for specific IS/3 features (especially advanced color management).

## 3. Visual Template Builder - Key Features & Output

*   **Essential Features:**
    *   **Web-Based Interface:** Accessible via a browser.
    *   **Drag-and-Drop Functionality:** Users can drag predefined elements (text boxes, image placeholders, barcode elements, chart areas) onto a canvas representing the document page.
    *   **WYSIWYG (What You See Is What You Get) or WYSIWYM (What You See Is What You Mean):** A visual representation of the template layout.
    *   **Element Properties Panel:** Allows users to configure properties for selected elements:
        *   **Text:** Font, size, color, alignment, static text, placeholder name for dynamic text.
        *   **Images:** Placeholder name, default image, scaling options.
        *   **Barcodes:** Barcode type (e.g., Code 128, QR Code), placeholder for data, dimension properties.
        *   **Charts:** Chart type (bar, line, pie), data source placeholder, axis labels, color schemes.
        *   **Shapes/Lines:** Basic drawing tools for static elements.
        *   **Positioning & Sizing:** X/Y coordinates, width, height, layers (bring to front, send to back).
        *   **Rotation:** Ability to rotate elements.
    *   **Dynamic Content Placeholders:** Clearly defined placeholders (e.g., `{{customer_name}}`, `{{product_image_url}}`, `{{transaction_barcode_data}}`) that will be populated by the Data Mapping Service.
    *   **Grid & Snapping:** To aid in aligning elements.
    *   **Zoom Functionality:** For detailed work.
    *   **Multi-page Support:** Ability to design templates spanning multiple pages with headers/footers.
    *   **Preview:** A basic preview of the template structure (without real data initially, or with sample data).
    *   **Save/Load Functionality:** To save template designs and load existing ones.
*   **Expected Output Format of the Template Design:**
    *   **JSON:** A JSON-based schema is recommended.
        *   **Reasoning:** JSON is lightweight, human-readable, and easily parsable by web technologies (JavaScript for the builder, Java/ .NET for the backend services). It can represent the nested structure of a template and its elements effectively.
        *   **Example Snippet (Conceptual):**
            ```json
            {
              "templateName": "InvoiceTemplate_v1",
              "pageSize": {"width": "210mm", "height": "297mm"},
              "elements": [
                {
                  "type": "textBox",
                  "id": "customerNameLabel",
                  "x": 10, "y": 10, "width": 50, "height": 5,
                  "font": "Arial", "size": 12, "color": "black",
                  "text": "Customer Name:",
                  "isPlaceholder": false
                },
                {
                  "type": "textBox",
                  "id": "customerNameValue",
                  "x": 65, "y": 10, "width": 100, "height": 5,
                  "font": "Arial", "size": 12, "color": "black",
                  "placeholder": "{{customer_name}}"
                },
                {
                  "type": "image",
                  "id": "logo",
                  "x": 150, "y": 5, "width": 40, "height": 20,
                  "placeholder": "{{logo_url}}"
                }
                // ... other elements
              ]
            }
            ```
    *   This JSON output will then be interpreted by the **Template Management Service** and ultimately used by the **AFP Generation Service** (likely by transforming this JSON into XSL-FO for Apache FOP).

## 4. Data Mapping Process

*   **Data Ingestion:**
    *   The **Data Mapping Service** will expose endpoints to receive data in JSON or XML formats.
    *   For database sources, it will require connection details and queries (potentially configured per template or per data source). It can use JDBC/ODBC drivers for connectivity.
*   **Mapping to Template Placeholders:**
    *   The service will fetch the template structure (the JSON output from the Visual Template Builder) from the **Template Management Service**.
    *   A mapping configuration will define how fields from the input data source (JSON keys, XML XPath expressions, database column names) correspond to the placeholders in the template JSON. This mapping could be:
        *   **Convention-based:** Input data fields directly match placeholder names (e.g., JSON key `"customer_name"` maps to `{{customer_name}}`).
        *   **Explicitly configured:** A separate mapping definition (e.g., another JSON object or UI configuration) that links source fields to template placeholders if names differ.
            ```json
            // Example Explicit Mapping
            {
              "templatePlaceholder": "{{customer_name}}",
              "dataSourceField": "client.details.fullName" // JSONPath for JSON
            }
            ```
*   **Basic Data Transformations:**
    *   **Direct Value Substitution:** Most common, where data is directly inserted.
    *   **Formatting:**
        *   **Dates:** `YYYY-MM-DD` to `MM/DD/YYYY`.
        *   **Numbers:** Decimal formatting, currency symbols.
        *   **Text:** Uppercase, lowercase, substring.
    *   **Concatenation:** Combining multiple input fields into one placeholder (e.g., `{{firstName}}` and `{{lastName}}` into `{{fullName}}`).
    *   **Conditional Logic (Simple):** Basic if/else logic for showing/hiding elements or choosing between values based on input data (e.g., if `country == "US"` use "$", else use "€"). Complex logic should ideally be handled in the data source or a dedicated pre-processing step.
    *   **Lookup/Enrichment (Limited):** Simple key-value lookups (e.g., mapping a status code `1` to `"Active"`). More complex data enrichment should be done before data is sent to this service.
*   **Transformation Tools:**
    *   Libraries like Apache Commons Lang (Java), JSLT (JSON transformations), or custom scripting capabilities (e.g., embedded JavaScript engine like GraalVM or Nashorn if using Java) can be used within the Data Mapping Service.
    *   For XML, XSLT can be a powerful transformation tool before mapping.

## 5. Template Version Control Strategy

*   **Mechanism:** **Git-like versioning system** is recommended, but managed by the **Template Management Service**.
    *   **Full Snapshots:** Each "commit" or save of a template (even minor changes from the Visual Template Builder) creates a new, immutable version of the template's JSON definition.
    *   **Sequential Version Numbers & Unique IDs:** Each version will have:
        *   A human-readable sequential version number (e.g., v1, v2, v2.1).
        *   A unique identifier (e.g., UUID or a hash of the content) for unambiguous referencing.
    *   **Tags/Labels:** Ability to tag specific versions with labels like "production", "latest_stable", "archived".
*   **Storage:**
    *   The **Template Management Service** will store these template JSON versions.
    *   **Database:** A relational or NoSQL database can be used.
        *   A `Templates` table storing the template's current/master information.
        *   A `TemplateVersions` table storing each version's JSON content, version number, timestamp, unique ID, and foreign key to the `Templates` table.
    *   **Dedicated Git Repository (Alternative/Hybrid):** The service could internally use a Git repository to store the JSON files. This provides robust versioning, diffing, and branching capabilities out-of-the-box. The service would then abstract Git operations via its API.
*   **Access:**
    *   Clients (like the AFP Generation Service) can request a specific template by:
        *   Unique version ID (most precise).
        *   Sequential version number (e.g., "InvoiceTemplate_v2").
        *   Tag/Label (e.g., "InvoiceTemplate_production"). If a tag is requested, the service resolves it to the specific unique version ID.
    *   The Visual Template Builder will load the latest version by default but should allow users to view, load, and potentially revert to older versions.

## 6. Multi-language Support Strategy

*   **Text Management:**
    *   **Externalized Strings:** Text elements in templates should not contain hardcoded display text directly in the template JSON if they need to be translated. Instead, they should use **localization keys**.
        *   Example in template JSON:
            ```json
            {
              "type": "textBox",
              "id": "greeting",
              "textKey": "template.invoice.greeting" // Instead of "text": "Hello"
              // ... other properties
            }
            ```
    *   **Resource Bundles/Translation Files:** For each supported language, a separate file (e.g., JSON, .properties, XML) will store the translations for these keys.
        *   `en.json`: `{ "template.invoice.greeting": "Hello", "template.invoice.total": "Total Amount" }`
        *   `fr.json`: `{ "template.invoice.greeting": "Bonjour", "template.invoice.total": "Montant total" }`
    *   **Language Selection:** The AFP generation request will need to include a language parameter (e.g., "en-US", "fr-FR").
    *   **Resolution:** The **Data Mapping Service** or **AFP Generation Service** will use this language parameter to load the appropriate resource bundle and substitute the `textKey` with the correct translation before sending it to the AFP rendering engine.
*   **Font Considerations:**
    *   **Unicode Support:** The chosen AFP generation technology (Apache FOP) and the fonts used must fully support Unicode to render characters from various languages correctly.
    *   **Font Availability:** The **Resource Management Service** must store and provide fonts that cover the character sets of all supported languages. This might involve:
        *   Using comprehensive fonts (e.g., Noto Sans by Google).
        *   Allowing template designers to specify different fonts for different languages if a single font doesn't cover all scripts optimally.
        *   Ensuring these fonts are correctly embedded or referenced in the AFP output as per MO:DCA IS/3 specifications for interchange.
    *   **Font Configuration in FOP:** Apache FOP has mechanisms to configure fonts, including embedding and subsetting, which will be crucial for multi-language documents.
*   **Regional Variations:**
    *   The system should also handle regional variations (e.g., date formats, number formats, currency symbols) which can be part of the localization data and applied by the Data Mapping Service.
*   **Template Builder UI:** The Visual Template Builder itself should also be internationalized to allow users from different regions to use it in their preferred language. This is separate from the multi-language support in the generated AFPs but important for usability.

This document provides a foundational design. Further details will be refined during the implementation phases of each microservice.

---
## 正體中文 (Traditional Chinese)

# 核心 AFP 產生引擎 - 設計文件

## 1. 建議架構

*   **架構模式：** 建議採用微服務架構。這將有助於系統不同元件的可擴展性、彈性以及獨立開發和部署。
*   **主要服務/元件：**
    *   **範本管理服務：**
        *   **描述：** 負責儲存、擷取和管理 AFP 範本。這包括處理範本版本和中繼資料。它將公開用於建立、更新、刪除和列出範本的 API。
        *   **技術建議：** Java Spring Boot 或 .NET Core Web API。
    *   **資料對應服務：**
        *   **描述：** 處理來自各種來源 (JSON、XML、資料庫) 的資料擷取，並將此資料對應到範本中定義的預留位置。它還將執行基本的資料轉換。
        *   **技術建議：** Java Spring Boot 或 .NET Core，可能使用像 Apache Camel 這樣的函式庫進行資料轉換和整合。
    *   **AFP 產生服務：**
        *   **描述：** 取得範本 (包含已對應的資料) 並產生最終的 AFP 文件。此服務將封裝所選的 AFP 產生函式庫/框架。它還將處理資源嵌入 (字型、影像)。
        *   **技術建議：** 使用 Apache FOP 的 Java 應用程式。
    *   **資源管理服務：**
        *   **描述：** 管理 AFP 產生所需的共用資源，如字型、影像和其他資產。這可確保資源有效儲存並可供 AFP 產生服務存取。
        *   **技術建議：** 可以是專用服務或整合到範本管理服務中。儲存可以利用雲端解決方案 (如 AWS S3、Azure Blob 儲存) 或本機檔案系統/資料庫。
    *   **API 閘道：**
        *   **描述：** 所有用戶端請求的單一進入點。它將請求路由到適當的後端服務、處理驗證，並可能執行某些請求/回應轉換。
        *   **技術建議：** Spring Cloud Gateway、Netflix Zuul 或雲端供應商的 API 閘道解決方案。
*   **服務間通訊方法：**
    *   **同步：** REST API (HTTP/HTTPS) 將是服務之間請求/回應互動的主要方法 (例如 API 閘道到範本管理、資料對應到範本管理)。
    *   **非同步：** 訊息佇列 (例如 RabbitMQ、Apache Kafka、AWS SQS、Azure Service Bus) 可用於解耦服務和處理長時間執行的程序，例如實際的 AFP 產生請求。例如，API 閘道可以將產生請求放置在佇列中，然後由 AFP 產生服務拾取。這可以提高彈性和可擴展性。

## 2. AFP 產生技術

*   **建議函式庫/框架：Apache FOP (格式化物件處理器)**
    *   **理由：**
        *   **基於 Java：** 可直接與 Java 應用程式整合，這是核心需求。
        *   **AFP 輸出：** 明確支援 AFP 作為輸出格式。文件指出符合各種標準，雖然 MO:DCA IS/3 的具體細節需要在實作過程中進一步驗證，但 FOP 的可擴充特性允許在需要時進行自訂。
        *   **XSL-FO 驅動：** FOP 使用 XSL-FO (可延伸樣式表語言格式化物件) 作為其輸入。XSL-FO 是一種強大的 W3C 標準，用於描述頁面版面配置和內容，允許複雜的文件結構。
        *   **開源：** 作為開源軟體 (Apache 授權 2.0)，消除了授權成本並提供了彈性。
        *   **成熟且文件齊全：** FOP 是一個成熟的專案，擁有大量文件和社群，有助於開發和疑難排解。
        *   **多種輸出格式：** 雖然 AFP 是目標格式，但 FOP 能夠輸出到 PDF、PS 等格式，這對於測試或替代使用案例可能很有用。
*   **替代方案 (商業 - 如果 FOP 有限制則列入考量)：**
    *   **IBM Document Composition Facility (DCF) 或來自主要列印供應商 (例如 Ricoh、Xerox) 的類似 AFP 公用程式。**
    *   **理由：** 這些是專為強大 AFP 建立而設計的業界標準工具，可保證符合 MO:DCA IS/3。但是，它們會產生授權成本，並且整合可能更複雜。如果開源選項證明不足以滿足特定的 IS/3 功能 (尤其是進階色彩管理)，則這將是後備方案。

## 3. 視覺化範本產生器 - 主要功能與輸出

*   **必要功能：**
    *   **網頁式介面：** 可透過瀏覽器存取。
    *   **拖放功能：** 使用者可以將預先定義的元素 (文字方塊、影像預留位置、條碼元素、圖表區域) 拖到代表文件頁面的畫布上。
    *   **WYSIWYG (所見即所得) 或 WYSIWYM (所見即所指)：** 範本版面配置的視覺化表示。
    *   **元素屬性面板：** 允許使用者設定所選元素的屬性：
        *   **文字：** 字型、大小、顏色、對齊方式、靜態文字、動態文字的預留位置名稱。
        *   **影像：** 預留位置名稱、預設影像、縮放選項。
        *   **條碼：** 條碼類型 (例如 Code 128、QR 碼)、資料的預留位置、尺寸屬性。
        *   **圖表：** 圖表類型 (長條圖、折線圖、圓餅圖)、資料來源預留位置、座標軸標籤、色彩配置。
        *   **形狀/線條：** 用於靜態元素的基本繪圖工具。
        *   **定位與大小調整：** X/Y 座標、寬度、高度、圖層 (移到最前、移到最後)。
        *   **旋轉：** 能夠旋轉元素。
    *   **動態內容預留位置：** 明確定義的預留位置 (例如 `{{customer_name}}`、`{{product_image_url}}`、`{{transaction_barcode_data}}`)，將由資料對應服務填入。
    *   **格線與貼齊：** 協助對齊元素。
    *   **縮放功能：** 用於細部工作。
    *   **多頁支援：** 能夠設計跨多個頁面並包含頁首/頁尾的範本。
    *   **預覽：** 範本結構的基本預覽 (最初不含真實資料，或使用範例資料)。
    *   **儲存/載入功能：** 儲存範本設計並載入現有設計。
*   **範本設計的預期輸出格式：**
    *   **JSON：** 建議使用基於 JSON 的結構描述。
        *   **理由：** JSON 輕量、易於人工讀取，並且易於由 Web 技術 (產生器的 JavaScript、後端服務的 Java/.NET) 剖析。它可以有效地表示範本及其元素的巢狀結構。
        *   **範例片段 (概念性)：**
            ```json
            {
              "templateName": "InvoiceTemplate_v1",
              "pageSize": {"width": "210mm", "height": "297mm"},
              "elements": [
                {
                  "type": "textBox",
                  "id": "customerNameLabel",
                  "x": 10, "y": 10, "width": 50, "height": 5,
                  "font": "Arial", "size": 12, "color": "black",
                  "text": "Customer Name:",
                  "isPlaceholder": false
                },
                {
                  "type": "textBox",
                  "id": "customerNameValue",
                  "x": 65, "y": 10, "width": 100, "height": 5,
                  "font": "Arial", "size": 12, "color": "black",
                  "placeholder": "{{customer_name}}"
                },
                {
                  "type": "image",
                  "id": "logo",
                  "x": 150, "y": 5, "width": 40, "height": 20,
                  "placeholder": "{{logo_url}}"
                }
                // ... 其他元素
              ]
            }
            ```
    *   此 JSON 輸出隨後將由**範本管理服務**解譯，並最終由 **AFP 產生服務**使用 (可能透過將此 JSON 轉換為 Apache FOP 的 XSL-FO)。

## 4. 資料對應程序

*   **資料擷取：**
    *   **資料對應服務**將公開端點以接收 JSON 或 XML 格式的資料。
    *   對於資料庫來源，它將需要連線詳細資訊和查詢 (可能依範本或資料來源設定)。它可以使用 JDBC/ODBC 驅動程式進行連線。
*   **對應到範本預留位置：**
    *   服務將從**範本管理服務**擷取範本結構 (視覺化範本產生器的 JSON 輸出)。
    *   對應組態將定義輸入資料來源中的欄位 (JSON 金鑰、XML XPath 運算式、資料庫欄位名稱) 如何對應到範本 JSON 中的預留位置。此對應可以是：
        *   **基於慣例：** 輸入資料欄位直接符合預留位置名稱 (例如 JSON 金鑰 `"customer_name"` 對應到 `{{customer_name}}`)。
        *   **明確設定：** 如果名稱不同，則使用單獨的對應定義 (例如另一個 JSON 物件或 UI 組態) 將來源欄位連結到範本預留位置。
            ```json
            // 範例明確對應
            {
              "templatePlaceholder": "{{customer_name}}",
              "dataSourceField": "client.details.fullName" // JSON 的 JSONPath
            }
            ```
*   **基本資料轉換：**
    *   **直接值替換：** 最常見，直接插入資料。
    *   **格式化：**
        *   **日期：** `YYYY-MM-DD` 轉為 `MM/DD/YYYY`。
        *   **數字：** 小數格式化、貨幣符號。
        *   **文字：** 大寫、小寫、子字串。
    *   **串連：** 將多個輸入欄位合併到一個預留位置 (例如 `{{firstName}}` 和 `{{lastName}}` 合併到 `{{fullName}}`)。
    *   **條件式邏輯 (簡單)：** 基本的 if/else 邏輯，用於根據輸入資料顯示/隱藏元素或在值之間進行選擇 (例如，如果 `country == "US"` 則使用 "$"，否則使用 "€")。複雜邏輯最好在資料來源或專用的預先處理步驟中處理。
    *   **查閱/充實 (有限)：** 簡單的鍵值查閱 (例如將狀態碼 `1` 對應到 `"Active"`)。更複雜的資料充實應在資料傳送到此服務之前完成。
*   **轉換工具：**
    *   可以在資料對應服務中使用像 Apache Commons Lang (Java)、JSLT (JSON 轉換) 或自訂腳本功能 (例如，如果使用 Java，則使用像 GraalVM 或 Nashorn 這樣的嵌入式 JavaScript 引擎) 等函式庫。
    *   對於 XML，XSLT 在對應之前可以作為強大的轉換工具。

## 5. 範本版本控制策略

*   **機制：** 建議使用**類似 Git 的版本控制系統**，但由**範本管理服務**管理。
    *   **完整快照：** 範本的每次「提交」或儲存 (即使是視覺化範本產生器的微小變更) 都會建立範本 JSON 定義的一個新的、不可變的版本。
    *   **循序版本號碼與唯一 ID：** 每個版本都將具有：
        *   人類可讀的循序版本號碼 (例如 v1、v2、v2.1)。
        *   唯一的識別碼 (例如 UUID 或內容的雜湊值)，以便明確參考。
    *   **標籤/標記：** 能夠使用像 "production"、"latest_stable"、"archived" 這樣的標籤來標記特定版本。
*   **儲存：**
    *   **範本管理服務**將儲存這些範本 JSON 版本。
    *   **資料庫：** 可以使用關聯式或 NoSQL 資料庫。
        *   `Templates` 資料表儲存範本的目前/主要資訊。
        *   `TemplateVersions` 資料表儲存每個版本的 JSON 內容、版本號碼、時間戳記、唯一 ID 以及指向 `Templates` 資料表的外鍵。
    *   **專用 Git 儲存庫 (替代/混合方案)：** 服務可以在內部使用 Git 儲存庫來儲存 JSON 檔案。這提供了健全的版本控制、差異比較和分支功能。然後，服務將透過其 API 抽象化 Git 操作。
*   **存取：**
    *   用戶端 (如 AFP 產生服務) 可以透過以下方式請求特定範本：
        *   唯一版本 ID (最精確)。
        *   循序版本號碼 (例如 "InvoiceTemplate_v2")。
        *   標籤/標記 (例如 "InvoiceTemplate_production")。如果請求標籤，服務會將其解析為特定的唯一版本 ID。
    *   視覺化範本產生器預設會載入最新版本，但應允許使用者檢視、載入並可能還原到較舊版本。

## 6. 多語言支援策略

*   **文字管理：**
    *   **外部化字串：** 如果範本中的文字元素需要翻譯，則不應在範本 JSON 中直接包含硬式編碼的顯示文字。相反，它們應使用**本地化金鑰**。
        *   範本 JSON 中的範例：
            ```json
            {
              "type": "textBox",
              "id": "greeting",
              "textKey": "template.invoice.greeting" // 而非 "text": "Hello"
              // ... 其他屬性
            }
            ```
    *   **資源包/翻譯檔案：** 對於每種支援的語言，將使用單獨的檔案 (例如 JSON、.properties、XML) 儲存這些金鑰的翻譯。
        *   `en.json`：`{ "template.invoice.greeting": "Hello", "template.invoice.total": "Total Amount" }`
        *   `fr.json`：`{ "template.invoice.greeting": "Bonjour", "template.invoice.total": "Montant total" }`
    *   **語言選擇：** AFP 產生請求將需要包含語言參數 (例如 "en-US"、"fr-FR")。
    *   **解析：****資料對應服務**或 **AFP 產生服務**將使用此語言參數載入適當的資源包，並在將 `textKey` 傳送到 AFP 呈現引擎之前將其替換為正確的翻譯。
*   **字型考量：**
    *   **Unicode 支援：** 所選的 AFP 產生技術 (Apache FOP) 和使用的字型必須完全支援 Unicode，才能正確呈現各種語言的字元。
    *   **字型可用性：****資源管理服務**必須儲存並提供涵蓋所有支援語言字元集的字型。這可能涉及：
        *   使用全面的字型 (例如 Google 的 Noto Sans)。
        *   如果單一字型無法最佳化涵蓋所有腳本，則允許範本設計人員為不同語言指定不同字型。
        *   根據 MO:DCA IS/3 規格，確保這些字型在 AFP 輸出中正確嵌入或參考以供交換。
    *   **FOP 中的字型組態：** Apache FOP 具有設定字型的機制，包括嵌入和子集化，這對於多語言文件至關重要。
*   **區域差異：**
    *   系統還應處理區域差異 (例如日期格式、數字格式、貨幣符號)，這些差異可以是本地化資料的一部分，並由資料對應服務套用。
*   **範本產生器 UI：** 視覺化範本產生器本身也應進行國際化，以允許不同地區的使用者以其偏好的語言使用它。這與產生的 AFP 中的多語言支援是分開的，但對於可用性而言很重要。

本文件提供了基礎設計。每個微服務的實作階段將會進一步完善細節。
