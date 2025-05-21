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
