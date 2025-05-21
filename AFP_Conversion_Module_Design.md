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
