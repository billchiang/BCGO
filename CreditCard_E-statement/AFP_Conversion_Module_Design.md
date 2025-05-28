# AFP to Other Formats Conversion Module - Design Document

## 1. Proposed Architecture

*   **Architectural Approach:** A dedicated **microservice**, named "AFP Conversion Service," is proposed.
    *   **Reasoning:** This approach offers several advantages:
        *   **Decoupling:** Isolates the complex logic of AFP conversion from other parts of the system (like the Core AFP Generation Engine or any front-end applications).
        *   **Scalability:** The conversion service can be scaled independently based on the conversion load, which can be resource-intensive.
        *   **Technology Flexibility:** Allows for the use of specific technologies or languages best suited for AFP processing (e.g., Java or .NET libraries, or even wrapping command-line tools) without impacting other services.
        *   **Maintainability:** Easier to update and maintain the conversion logic and its dependencies.
*   **Receiving AFP Files and Outputting Converted Files:**
    *   **Input (Receiving AFP):**
        *   **API Endpoint:** The service will expose a RESTful API endpoint (e.g., `/convert`) that accepts AFP files.
            *   **Method 1 (Preferred for smaller files):** `POST` request with `multipart/form-data` containing the AFP file and target format parameters.
            *   **Method 2 (For larger files or asynchronous processing):** `POST` request with a JSON payload specifying a URI to the AFP file (e.g., a path in a shared storage like S3, Azure Blob Storage, or a network file system) and target format parameters. The service would then fetch the file from this URI.
        *   **Message Queue (for batch/asynchronous):** The service can listen to a message queue (e.g., RabbitMQ, Kafka, AWS SQS) for conversion requests. Each message would contain metadata similar to Method 2 (URI to AFP, target format, output location, callback URL/ID).
    *   **Output (Delivering Converted Files):**
        *   **Synchronous (for on-demand/small files via API):** The API can return the converted file directly in the HTTP response if the conversion is quick.
        *   **Asynchronous (for larger files, batch, or long conversions):**
            *   The API can return a job ID immediately. A separate endpoint (e.g., `/status/{job_id}`) can be polled for completion.
            *   Upon completion, the converted file can be:
                *   Stored in a pre-configured location (e.g., S3 bucket, Azure Blob, specific file server directory) with the path returned in the status response or via a callback.
                *   A callback URL (provided in the initial request) can be invoked with the link to the converted file.
                *   If initiated via a message queue, a "completion" message can be sent to another queue with the output details.
    *   **Parameters in Request:**
        *   `target_format`: (Required) e.g., "pdf", "pdfa-2b", "pcl", "ps".
        *   `source_afp_uri`: (Required if not uploading directly) Location of the AFP file.
        *   `output_uri_template`: (Optional) Template for naming/storing the output file.
        *   `ocr_required`: (Optional, defaults to false) Boolean, to indicate if OCR should be attempted.
        *   `pdfa_compliance_level`: (Optional, if target is PDF/A) e.g., "2b", "3u".
        *   `callback_url`: (Optional) For asynchronous notifications.

## 2. Conversion Technologies & Libraries

This section explores options, noting that direct AFP manipulation is a niche area, often dominated by commercial tools due to its complexity and legacy.

*   **AFP to PDF & PDF/A:**
    *   **Commercial Tools (Recommended for highest fidelity and MO:DCA IS/3 support):**
        *   **Crawford Technologies (e.g., PRO Transform Plus, Transformation Server):**
            *   **Pros:** Industry leader, extensive experience with AFP (including MO:DCA IS/3), high-fidelity conversions, robust handling of resources (fonts, overlays, page segments), color management (FS45 for IOCA), support for PDF/A standards, enterprise-grade reliability and support. Often provide GUIs for configuration and command-line interfaces for integration.
            *   **Cons:** Cost (licensing fees). Can be complex to configure initially.
        *   **OpenText (formerly Actuate, Xenos, StreamServe):**
            *   **Pros:** Strong suite for enterprise document management and transformation, good AFP support, reliable for high-volume environments.
            *   **Cons:** Cost, potentially part of a larger platform rather than a standalone lightweight converter.
        *   **Precisely (formerly Syncsort, Pitney Bowes Software - EngageOne Conversion Hub):**
            *   **Pros:** Known for data integration and document processing, offers AFP conversion capabilities.
            *   **Cons:** Cost.
        *   **Compart (DocBridge Mill):**
            *   **Pros:** Specializes in document and output management, strong support for various print streams including AFP to PDF/PDF/A. Good for complex transformations and high volumes.
            *   **Cons:** Cost.
        *   **Why Commercial is Often Preferred for AFP:** AFP is a complex, record-oriented format. MO:DCA IS/3 introduced advanced features like object containers, sophisticated color (FS45), and complex resource management. Commercial vendors have invested heavily in reverse-engineering and supporting these nuances over many years.
    *   **Open-Source Libraries (Limited options, may not fully support all MO:DCA IS/3 features):**
        *   **Apache FOP (Formatting Objects Processor):**
            *   **Pros:** While primarily an XSL-FO to output-format tool, FOP *can* render AFP. If the AFP structure is relatively simple or can be pre-parsed into an intermediate representation that FOP understands, it might be a pathway. It supports PDF/A output.
            *   **Cons:** Not a direct AFP-to-PDF converter in the traditional sense. Full MO:DCA IS/3 fidelity, especially for complex resources and color, is highly unlikely without significant custom development. Its AFP input capabilities are less mature than its output. Reliability for complex enterprise AFPs would be a major concern.
        *   **Ghostscript (with AFP input plugin - if available and mature):**
            *   **Pros:** Powerful open-source interpreter for PostScript and PDF. Some versions or forks *might* have experimental AFP input support through add-ons (e.g., `afppl` was a historical project, its current status is unclear). If a viable plugin exists, Ghostscript's PDF generation is robust.
            *   **Cons:** Finding a mature and reliable open-source AFP input filter for Ghostscript that supports rich MO:DCA IS/3 is difficult. Likely to have limitations in resource handling and fidelity.
    *   **Recommendation for PDF & PDF/A:** Start by evaluating leading **commercial tools** due to the complexity of AFP and MO:DCA IS/3. An open-source approach would likely require substantial R&D and may not achieve the required fidelity for enterprise use.

*   **AFP to PCL & PostScript:**
    *   **Commercial Tools (Generally the same vendors as above):**
        *   **Crawford Technologies, OpenText, Precisely, Compart:** These vendors typically support transformations from AFP to PCL and PostScript as part of their product suites.
            *   **Pros:** High fidelity, resource handling, reliability, and support.
            *   **Cons:** Cost.
    *   **Open-Source Libraries:**
        *   This is even more challenging than AFP to PDF in the open-source realm. Direct, high-fidelity AFP to PCL/PostScript open-source converters are rare or non-existent.
        *   **Possible Multi-Step Process (Lower Fidelity):** One theoretical, but likely lower-fidelity, route could be AFP -> PDF (using whatever tool, even if imperfect) -> PCL/PostScript (using tools like Ghostscript). This would likely lose AFP-specific information and print controls.
    *   **Recommendation for PCL & PostScript:** **Commercial tools** are strongly recommended. The intricacies of converting AFP's object-based structure and resource model to PCL or PostScript accurately usually require specialized commercial software.

*   **Maturity and Reliability:**
    *   **Commercial Tools:** Generally mature and reliable, designed for enterprise-scale, high-volume, and mission-critical operations. They come with support contracts and a history of handling complex legacy print streams.
    *   **Open-Source Tools for AFP Conversion:** Largely immature or non-existent for direct, high-fidelity conversion of complex AFPs. While components like Apache FOP or Ghostscript are mature for *their primary tasks*, their application to AFP conversion is often indirect or limited.

## 3. Layout and Content Fidelity

*   **Resource Management (Key Challenge):**
    *   **External Resources:** AFP documents heavily rely on external resources:
        *   **Fonts (FOCA):** Character sets, code pages, font control records. The conversion tool must be able to locate, parse, and correctly map/embed these fonts in the target format (e.g., TrueType/OpenType in PDF). Missing fonts or incorrect mapping is a major source of fidelity loss.
        *   **Page Segments (PSEGs):** Pre-composed snippets of text and/or images.
        *   **Overlays:** Standard content applied to multiple pages (e.g., forms, letterheads).
        *   **Object Containers (IOCA for FS10/FS11 images, FS45 for color):** For images and graphical objects.
    *   **Strategy:**
        *   **Resource Libraries:** The conversion service needs access to the same AFP resource libraries (font libraries, PSEG/Overlay libraries) that were used when the AFP was originally generated. This might involve configuring paths to these libraries.
        *   **Embedding vs. Referencing:** For PDF/PDF/A, all necessary resources (especially fonts and images) should be embedded to ensure fidelity across different systems. For PCL/PostScript, resources might be downloaded to the printer or embedded.
        *   **Commercial tools excel here**, as they often have sophisticated mechanisms for managing and resolving these external AFP resources.
*   **Color Management:**
    *   **MO:DCA IS/3 supports advanced color spaces (e.g., CMYK, spot colors via FS45).**
    *   **Strategy:** The conversion tool must accurately interpret AFP color specifications and map them to the target format's color model. ICC profiles, if used in AFP, should be preserved or appropriately converted. PDF/A has specific color requirements.
*   **Layout and Positioning:**
    *   AFP uses precise coordinate systems. The conversion must maintain the exact positioning and sizing of text and graphical elements.
    *   **Strategy:** The chosen tool must have a robust layout engine capable of translating AFP constructs (e.g., BDT/EDT for text blocks, GOCA for graphics) to the target format's drawing commands.
*   **Text Control:**
    *   AFP has sophisticated text controls (e.g., inter-character spacing, baseline orientation).
    *   **Strategy:** The converter needs to replicate these controls as closely as possible in the target format.
*   **Conditional Processing (PTX):**
    *   AFP can contain conditional logic. While conversion usually deals with a "composed" AFP, if any PTX records are still present and need interpretation, this adds complexity. Most converters expect fully resolved AFP.
    *   **Strategy:** Assume input AFPs are fully composed. If not, a pre-processing step might be needed, or the tool must support this (commercial tools are more likely to).

## 4. Batch vs. Real-Time Processing

The architecture using API endpoints and Message Queues naturally supports both:

*   **Real-Time (On-Demand) Single-File Conversions:**
    *   **Mechanism:** Client sends a request to the API endpoint with the AFP file (or URI to it).
    *   **Processing:**
        *   For small files and quick conversions, the service can process synchronously and return the converted file in the response.
        *   For larger files, the API returns a Job ID, and the client polls for status or waits for a callback.
    *   **Use Cases:** Interactive applications, user-initiated conversions, converting a small number of documents.
*   **High-Volume Batch Conversions:**
    *   **Mechanism:**
        *   **Option 1 (Message Queue):** A batch process (or another service) generates messages for each AFP file to be converted and puts them onto a dedicated message queue (e.g., SQS, RabbitMQ, Kafka). The AFP Conversion Service instances would consume messages from this queue.
        *   **Option 2 (Watched Folder + API):** A process drops AFP files into a designated "watch folder." A separate script or daemon monitors this folder and submits conversion requests to the API (likely using the URI method and asynchronous processing).
    *   **Processing:**
        *   The AFP Conversion Service processes these requests asynchronously.
        *   Converted files are stored in a specified output location (e.g., S3 bucket, shared directory).
        *   Completion status can be logged, sent to another queue, or updated in a database.
    *   **Scalability:** Multiple instances of the AFP Conversion Service can be run to process messages/requests in parallel, effectively handling high volumes.
    *   **Error Handling:** Robust error handling and retry mechanisms are crucial for batch processing. Failed conversions should be logged and potentially moved to an error queue/directory for investigation.
    *   **Use Cases:** Migrating large archives of AFP documents, nightly processing of generated AFP statements, feeding documents into content management systems.

## 5. Full-Text Search and Indexing Enablement

*   **Ensuring Searchable Text (Especially for PDF/PDF/A):**
    *   **AFP Text Objects:** AFP typically stores text in structured fields (BDT/EDT) with associated encoding (CPGID).
    *   **Conversion Process:**
        *   The converter must extract this text accurately, decode it using the correct code page, and then embed it as searchable text in the PDF (not as an image of text).
        *   Fonts should be embedded with proper ToUnicode mappings in the PDF to allow text extraction and search by standard PDF viewers and indexers.
        *   **Commercial tools are generally good at this.**
*   **Handling Image-Based AFPs (Scanned Documents or Faxed Documents Stored as AFP):**
    *   **Challenge:** Some AFPs might be containers for raster images (e.g., TIFFs wrapped in IOCA FS10/FS11) that represent scanned pages. These are not inherently searchable.
    *   **OCR (Optical Character Recognition) Integration:**
        *   **Detection:** The service might need a mechanism to detect if an AFP is primarily image-based (e.g., by analyzing the proportion of image objects vs. text objects, or by a flag in the request).
        *   **OCR Step:** If OCR is required (and requested via `ocr_required` parameter):
            *   After an initial conversion to an image format (like TIFF) or directly from the image objects within the AFP, an OCR engine would process these images.
            *   **Recommended OCR Engines:**
                *   **Tesseract OCR (Open Source):** Good quality, supports many languages, actively developed. Requires careful preprocessing of images for best results.
                *   **AWS Textract, Google Cloud Vision AI, Azure AI Vision (Commercial Cloud-based):** Excellent accuracy, scalable, but incurs costs.
                *   **Commercial SDKs (ABBYY FineReader Engine, Kofax OmniPage SDK):** High accuracy, robust, but licensed.
            *   The OCRed text would then be layered invisibly behind the image in the output PDF (a common technique for making scanned documents searchable).
*   **Integration with External Search Indexing Platforms (e.g., Elasticsearch, Solr):**
    *   **Workflow:**
        1.  AFP is converted to a searchable format (preferably PDF/A with actual text, or PDF with OCRed text layer).
        2.  The converted file is stored in a repository (CMS, file system, S3).
        3.  A separate process or the AFP Conversion Service (if configured) extracts the text content from the converted PDF. Libraries like Apache PDFBox (Java) or PyPDF2/pdfminer.six (Python) can do this.
        4.  This extracted text, along with metadata (filename, document ID, creation date, etc.), is then sent to the indexing platform (Elasticsearch, Solr, OpenSearch) via their respective APIs.
    *   **Metadata:** Crucial for effective searching. The conversion process should preserve or generate relevant metadata. The original AFP often contains metadata in structured fields (e.g., TLEs - Tagged Logical Elements) which, if extracted by the converter, can be passed along for indexing.
    *   **Connectors:** Some indexing platforms have connectors or ingestion pipelines (e.g., Elasticsearch Ingest Node with an attachment processor, Apache ManifoldCF) that can directly consume files, perform text extraction (often using Tika internally), and index them. The AFP Conversion Service would then just need to place the converted PDF in a location accessible to these connectors.

This design provides a framework for a robust AFP conversion module. The choice of specific conversion tools, especially commercial vs. open-source, will heavily depend on budget, the complexity of the AFP files (particularly MO:DCA IS/3 feature usage), and the required fidelity.

---
## 正體中文 (Traditional Chinese)

# AFP 至其他格式轉換模組 - 設計文件

## 1. 建議架構

*   **架構方法：** 建議採用名為「AFP 轉換服務」的專用**微服務**。
    *   **理由：** 此方法具有多項優點：
        *   **解耦：** 將複雜的 AFP 轉換邏輯與系統的其他部分（如核心 AFP 產生引擎或任何前端應用程式）隔離。
        *   **可擴展性：** 轉換服務可以根據轉換負載獨立擴展，轉換負載可能需要大量資源。
        *   **技術彈性：** 允許使用最適合 AFP 處理的特定技術或語言（例如 Java 或 .NET 函式庫，甚至包裝命令列工具），而不會影響其他服務。
        *   **可維護性：** 更易於更新和維護轉換邏輯及其相依性。
*   **接收 AFP 檔案與輸出轉換後檔案：**
    *   **輸入 (接收 AFP)：**
        *   **API 端點：** 服務將公開一個 RESTful API 端點 (例如 `/convert`)，用於接收 AFP 檔案。
            *   **方法 1 (較小檔案首選)：** 包含 AFP 檔案和目標格式參數的 `multipart/form-data` 的 `POST` 請求。
            *   **方法 2 (適用於較大檔案或非同步處理)：** 包含 JSON 有效負載的 `POST` 請求，指定 AFP 檔案的 URI (例如，S3、Azure Blob 儲存或網路檔案系統等共用儲存中的路徑) 和目標格式參數。然後服務將從此 URI 擷取檔案。
        *   **訊息佇列 (用於批次/非同步)：** 服務可以偵聽訊息佇列 (例如 RabbitMQ、Kafka、AWS SQS) 以取得轉換請求。每個訊息將包含類似於方法 2 的中繼資料 (AFP 的 URI、目標格式、輸出位置、回呼 URL/ID)。
    *   **輸出 (傳送轉換後的檔案)：**
        *   **同步 (透過 API 處理隨選/小型檔案)：** 如果轉換快速，API 可以在 HTTP 回應中直接傳回轉換後的檔案。
        *   **非同步 (適用於較大檔案、批次或長時間轉換)：**
            *   API 可以立即傳回工作 ID。可以輪詢單獨的端點 (例如 `/status/{job_id}`) 以了解完成情況。
            *   完成後，轉換後的檔案可以：
                *   儲存在預先設定的位置 (例如 S3 儲存貯體、Azure Blob、特定檔案伺服器目錄)，並在狀態回應中或透過回呼傳回路徑。
                *   可以使用指向轉換後檔案的連結來叫用回呼 URL (在初始請求中提供)。
                *   如果透過訊息佇列起始，則可以將包含輸出詳細資訊的「完成」訊息傳送到另一個佇列。
    *   **請求中的參數：**
        *   `target_format`：(必要) 例如 "pdf"、"pdfa-2b"、"pcl"、"ps"。
        *   `source_afp_uri`：(如果未直接上傳則為必要) AFP 檔案的位置。
        *   `output_uri_template`：(選用) 用於命名/儲存輸出檔案的範本。
        *   `ocr_required`：(選用，預設為 false) 布林值，表示是否應嘗試 OCR。
        *   `pdfa_compliance_level`：(選用，如果目標為 PDF/A) 例如 "2b"、"3u"。
        *   `callback_url`：(選用) 用於非同步通知。

## 2. 轉換技術與函式庫

本節探討各種選項，並指出由於其複雜性和歷史遺留問題，直接的 AFP 操作是一個利基領域，通常由商業工具主導。

*   **AFP 至 PDF 與 PDF/A：**
    *   **商業工具 (建議用於最高保真度和 MO:DCA IS/3 支援)：**
        *   **Crawford Technologies (例如 PRO Transform Plus、Transformation Server)：**
            *   **優點：** 產業領導者，在 AFP (包括 MO:DCA IS/3) 方面擁有豐富經驗，高保真度轉換，對資源 (字型、疊加、頁面區段) 的強大處理，色彩管理 (用於 IOCA 的 FS45)，支援 PDF/A 標準，企業級可靠性和支援。通常提供用於組態的 GUI 和用於整合的命令列介面。
            *   **缺點：** 成本 (授權費)。初始組態可能很複雜。
        *   **OpenText (前身為 Actuate、Xenos、StreamServe)：**
            *   **優點：** 強大的企業文件管理和轉換套件，良好的 AFP 支援，適用於高容量環境的可靠性。
            *   **缺點：** 成本，可能是一個較大平台的一部分，而不是獨立的輕量級轉換器。
        *   **Precisely (前身為 Syncsort、Pitney Bowes Software - EngageOne Conversion Hub)：**
            *   **優點：** 以資料整合和文件處理聞名，提供 AFP 轉換功能。
            *   **缺點：** 成本。
        *   **Compart (DocBridge Mill)：**
            *   **優點：** 專注於文件和輸出管理，強力支援各種列印流，包括 AFP 至 PDF/PDF/A。適用於複雜轉換和高容量。
            *   **缺點：** 成本。
        *   **為何 AFP 通常首選商業工具：** AFP 是一種複雜的、以記錄為導向的格式。MO:DCA IS/3 引入了進階功能，如物件容器、複雜色彩 (FS45) 和複雜的資源管理。商業供應商多年來投入大量資金進行反向工程並支援這些細微差別。
    *   **開源函式庫 (選項有限，可能無法完全支援所有 MO:DCA IS/3 功能)：**
        *   **Apache FOP (格式化物件處理器)：**
            *   **優點：** 雖然主要是一個 XSL-FO 到輸出格式的工具，但 FOP *可以* 呈現 AFP。如果 AFP 結構相對簡單，或者可以預先剖析為 FOP 理解的中間表示，則它可能是一條途徑。它支援 PDF/A 輸出。
            *   **缺點：** 在傳統意義上並非直接的 AFP 至 PDF 轉換器。若無大量客製化開發，完全的 MO:DCA IS/3 保真度，尤其是對於複雜資源和色彩，極不可能。其 AFP 輸入功能不如其輸出成熟。對於複雜的企業 AFP，可靠性將是一個主要問題。
        *   **Ghostscript (搭配 AFP 輸入外掛程式 - 如果可用且成熟)：**
            *   **優點：** 強大的 PostScript 和 PDF 開源直譯器。某些版本或分支*可能*透過附加元件 (例如 `afppl` 是一個歷史專案，其目前狀態不明) 提供實驗性的 AFP 輸入支援。如果存在可行的外掛程式，Ghostscript 的 PDF 產生功能很強大。
            *   **缺點：** 很難找到一個成熟可靠、支援豐富 MO:DCA IS/3 的 Ghostscript 開源 AFP 輸入篩選器。很可能在資源處理和保真度方面存在限制。
    *   **PDF 與 PDF/A 建議：** 由於 AFP 和 MO:DCA IS/3 的複雜性，請從評估領先的**商業工具**開始。開源方法可能需要大量研發，並且可能無法達到企業使用的保真度要求。

*   **AFP 至 PCL 與 PostScript：**
    *   **商業工具 (通常與上述供應商相同)：**
        *   **Crawford Technologies、OpenText、Precisely、Compart：** 這些供應商通常在其產品套件中支援從 AFP 到 PCL 和 PostScript 的轉換。
            *   **優點：** 高保真度、資源處理、可靠性和支援。
            *   **缺點：** 成本。
    *   **開源函式庫：**
        *   這在開源領域比 AFP 至 PDF 更具挑戰性。直接、高保真度的 AFP 至 PCL/PostScript 開源轉換器非常罕見或不存在。
        *   **可能的多步驟程序 (保真度較低)：** 一種理論上但保真度可能較低的途徑是 AFP -> PDF (使用任何工具，即使不完美) -> PCL/PostScript (使用像 Ghostscript 這樣的工具)。這很可能會遺失 AFP 特定資訊和列印控制。
    *   **PCL 與 PostScript 建議：** 強烈建議使用**商業工具**。準確地將 AFP 的基於物件的結構和資源模型轉換為 PCL 或 PostScript 的複雜性通常需要專門的商業軟體。

*   **成熟度與可靠性：**
    *   **商業工具：** 通常成熟可靠，專為企業級、高容量和關鍵任務操作而設計。它們提供支援合約，並且在處理複雜的舊式列印流方面擁有悠久歷史。
    *   **用於 AFP 轉換的開源工具：** 對於複雜 AFP 的直接、高保真度轉換，基本上不成熟或不存在。雖然像 Apache FOP 或 Ghostscript 這樣的元件在其*主要任務*上是成熟的，但它們在 AFP 轉換方面的應用通常是間接的或有限的。

## 3. 版面配置與內容保真度

*   **資源管理 (主要挑戰)：**
    *   **外部資源：** AFP 文件嚴重依賴外部資源：
        *   **字型 (FOCA)：** 字元集、字碼頁、字型控制記錄。轉換工具必須能夠找到、剖析這些字型，並在目標格式 (例如 PDF 中的 TrueType/OpenType) 中正確對應/嵌入這些字型。遺失字型或不正確的對應是保真度損失的主要原因。
        *   **頁面區段 (PSEGs)：** 預先組合的文字和/或影像片段。
        *   **疊加：** 套用於多個頁面的標準內容 (例如表單、信箋)。
        *   **物件容器 (用於 FS10/FS11 影像的 IOCA，用於色彩的 FS45)：** 用於影像和圖形物件。
    *   **策略：**
        *   **資源庫：** 轉換服務需要存取最初產生 AFP 時使用的相同 AFP 資源庫 (字型庫、PSEG/疊加庫)。這可能涉及設定這些庫的路徑。
        *   **嵌入與參考：** 對於 PDF/PDF/A，應嵌入所有必要的資源 (尤其是字型和影像)，以確保在不同系統上的保真度。對於 PCL/PostScript，資源可能會下載到印表機或嵌入。
        *   **商業工具在這方面表現出色**，因為它們通常具有用於管理和解析這些外部 AFP 資源的複雜機制。
*   **色彩管理：**
    *   **MO:DCA IS/3 支援進階色彩空間 (例如 CMYK、透過 FS45 的特別色)。**
    *   **策略：** 轉換工具必須準確解譯 AFP 色彩規格，並將其對應到目標格式的色彩模型。如果在 AFP 中使用 ICC 設定檔，則應保留或適當轉換。PDF/A 具有特定的色彩要求。
*   **版面配置與定位：**
    *   AFP 使用精確的座標系統。轉換必須保持文字和圖形元素的精確定位和大小。
    *   **策略：** 所選工具必須具有強大的版面配置引擎，能夠將 AFP 建構 (例如用於文字區塊的 BDT/EDT，用於圖形的 GOCA) 轉換為目標格式的繪圖指令。
*   **文字控制：**
    *   AFP 具有複雜的文字控制 (例如字元間距、基準線方向)。
    *   **策略：** 轉換器需要在目標格式中盡可能複製這些控制。
*   **條件式處理 (PTX)：**
    *   AFP 可以包含條件式邏輯。雖然轉換通常處理「已組合」的 AFP，但如果仍然存在任何 PTX 記錄並且需要解譯，則會增加複雜性。大多數轉換器期望完全解析的 AFP。
    *   **策略：** 假設輸入 AFP 是完全組合的。如果不是，則可能需要預先處理步驟，或者工具必須支援此功能 (商業工具更有可能)。

## 4. 批次與即時處理

使用 API 端點和訊息佇列的架構自然支援兩者：

*   **即時 (隨選) 單一檔案轉換：**
    *   **機制：** 用戶端將帶有 AFP 檔案 (或其 URI) 的請求傳送到 API 端點。
    *   **處理：**
        *   對於小型檔案和快速轉換，服務可以同步處理並在回應中傳回轉換後的檔案。
        *   對於較大的檔案，API 會傳回工作 ID，用戶端會輪詢狀態或等待回呼。
    *   **使用案例：** 互動式應用程式、使用者起始的轉換、轉換少量文件。
*   **高容量批次轉換：**
    *   **機制：**
        *   **選項 1 (訊息佇列)：** 批次程序 (或其他服務) 為每個要轉換的 AFP 檔案產生訊息，並將其放入專用訊息佇列 (例如 SQS、RabbitMQ、Kafka)。AFP 轉換服務執行個體將從此佇列取用訊息。
        *   **選項 2 (受監視資料夾 + API)：** 程序將 AFP 檔案放入指定的「受監視資料夾」。單獨的指令碼或精靈會監視此資料夾，並將轉換請求提交給 API (可能使用 URI 方法和非同步處理)。
    *   **處理：**
        *   AFP 轉換服務非同步處理這些請求。
        *   轉換後的檔案儲存在指定的輸出位置 (例如 S3 儲存貯體、共用目錄)。
        *   完成狀態可以記錄、傳送到另一個佇列或在資料庫中更新。
    *   **可擴展性：** 可以執行 AFP 轉換服務的多個執行個體以平行處理訊息/請求，有效地處理高容量。
    *   **錯誤處理：** 強大的錯誤處理和重試機制對於批次處理至關重要。失敗的轉換應記錄下來，並可能移至錯誤佇列/目錄以進行調查。
    *   **使用案例：** 遷移大型 AFP 文件存檔、夜間處理產生的 AFP 報表、將文件饋送至內容管理系統。

## 5. 全文搜尋與索引啟用

*   **確保可搜尋文字 (尤其是 PDF/PDF/A)：**
    *   **AFP 文字物件：** AFP 通常將文字儲存在結構化欄位 (BDT/EDT) 中，並具有相關編碼 (CPGID)。
    *   **轉換程序：**
        *   轉換器必須準確擷取此文字，使用正確的字碼頁對其進行解碼，然後將其作為可搜尋文字嵌入 PDF 中 (而不是作為文字影像)。
        *   應在 PDF 中嵌入具有適當 ToUnicode 對應的字型，以允許標準 PDF 檢視器和索引器進行文字擷取和搜尋。
        *   **商業工具通常在這方面做得很好。**
*   **處理基於影像的 AFP (儲存為 AFP 的掃描文件或傳真文件)：**
    *   **挑戰：** 某些 AFP 可能是點陣影像 (例如，包裝在 IOCA FS10/FS11 中的 TIFF) 的容器，這些影像是掃描頁面。這些本質上是不可搜尋的。
    *   **OCR (光學字元辨識) 整合：**
        *   **偵測：** 服務可能需要一種機制來偵測 AFP 是否主要基於影像 (例如，透過分析影像物件與文字物件的比例，或透過請求中的旗標)。
        *   **OCR 步驟：** 如果需要 OCR (並透過 `ocr_required` 參數請求)：
            *   在初始轉換為影像格式 (如 TIFF) 或直接從 AFP 中的影像物件轉換後，OCR 引擎將處理這些影像。
            *   **建議的 OCR 引擎：**
                *   **Tesseract OCR (開源)：** 品質良好，支援多種語言，積極開發中。需要仔細預先處理影像以獲得最佳效果。
                *   **AWS Textract、Google Cloud Vision AI、Azure AI Vision (商業雲端型)：** 準確性極佳，可擴展，但會產生費用。
                *   **商業 SDK (ABBYY FineReader Engine、Kofax OmniPage SDK)：** 準確性高，功能強大，但需要授權。
            *   然後，OCR 文字將不可見地分層在輸出 PDF 中的影像後面 (一種使掃描文件可搜尋的常用技術)。
*   **與外部搜尋索引平台 (例如 Elasticsearch、Solr) 整合：**
    *   **工作流程：**
        1.  AFP 轉換為可搜尋格式 (最好是帶有實際文字的 PDF/A，或帶有 OCR 文字層的 PDF)。
        2.  轉換後的檔案儲存在儲存庫 (CMS、檔案系統、S3) 中。
        3.  單獨的程序或 AFP 轉換服務 (如果已設定) 從轉換後的 PDF 中擷取文字內容。像 Apache PDFBox (Java) 或 PyPDF2/pdfminer.six (Python) 這樣的函式庫可以做到這一點。
        4.  然後，此擷取的文字連同中繼資料 (檔案名稱、文件 ID、建立日期等) 將透過其各自的 API 傳送到索引平台 (Elasticsearch、Solr、OpenSearch)。
    *   **中繼資料：** 對於有效搜尋至關重要。轉換程序應保留或產生相關的中繼資料。原始 AFP 通常在結構化欄位 (例如 TLE - 標記邏輯元素) 中包含中繼資料，如果轉換器擷取這些中繼資料，則可以將其傳遞以進行索引。
    *   **連接器：** 某些索引平台具有連接器或擷取管線 (例如，具有附件處理器的 Elasticsearch Ingest Node、Apache ManifoldCF)，可以直接取用檔案、執行文字擷取 (通常在內部使用 Tika) 並對其進行索引。然後，AFP 轉換服務只需將轉換後的 PDF 放置在這些連接器可存取的位置。

此設計為強大的 AFP 轉換模組提供了一個框架。特定轉換工具的選擇，尤其是商業工具與開源工具的選擇，將在很大程度上取決於預算、AFP 檔案的複雜性 (特別是 MO:DCA IS/3 功能的使用) 以及所需的保真度。
