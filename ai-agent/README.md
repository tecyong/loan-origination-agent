# 🌟 Gemini Nexus - Enterprise Spring Boot AI Agent

An autonomous, multi-tool AI Agent built with **Java 21**, **Spring Boot 3.3+**, and **Spring AI**, integrated with **Google Gemini 2.5 Flash**.

The agent features autonomous reasoning, declarative `@Tool` / function calling against live public and enterprise REST APIs, a local RAG Knowledge Base with in-memory `SimpleVectorStore` and disk persistence, session-based persistent memory, real-time Server-Sent Events (SSE) streaming, and a glassmorphic Web Chat UI.

---

## 🚀 Key Features

- **Google Gemini 2.5 Flash Core**: High-speed reasoning, function calling, and token generation via Google AI Studio API.
- **Dynamic & Domain REST API Tools**:
  - 🌦️ `weatherLookup`: Real-time worldwide weather and temperature forecasts via the Open-Meteo REST API.
  - 🏢 `crmLookup`: Enterprise mock CRM & ERP data (customer profiles, VIP tiers, order delivery tracking, warehouse inventory).
  - 🌐 `genericRestApi`: Dynamic HTTP client capable of executing arbitrary REST calls (GET, POST, etc.) to any external or internal service.
  - 📚 `knowledgeBaseSearch`: Semantic RAG retrieval querying policies, employee handbooks, and uploaded manuals.
- **RAG Knowledge Base**:
  - Backed by Spring AI `SimpleVectorStore` with zero-setup JSON persistence (`data/knowledge-store.json`).
  - Ultra-fast local dense vector embeddings (384 dimensions) with **zero API costs or rate-limits**.
  - Pre-seeded with company policies, return & refund terms, and API integration guides.
  - Dynamic drag-and-drop file upload endpoint supporting `.txt`, `.md`, and `.pdf` documents.
- **Multi-Session Persistent Chat Memory**:
  - Preserves conversation context across browser refreshes and application restarts in `data/chat-history.json`.
- **Full Interface Suite**:
  - **Modern Web Chat UI**: Glassmorphic dark design with real-time SSE streaming, quick prompt cards, and a slide-out RAG management drawer.
  - **OpenAPI & Swagger UI**: Interactive API documentation available at `/swagger-ui.html`.
  - **REST API**: Synchronous and streaming endpoints for programmatic integration.

---

## 🛠️ Prerequisites

- **Java**: JDK 21 or higher (Tested with OpenJDK 25)
- **Maven**: Apache Maven 3.8+ (Tested with Maven 3.9.16)
- **Google Gemini API Key**: Obtain a free API key from [Google AI Studio](https://aistudio.google.com/).

---

## ⚡ Quick Start

### 1. Set Your Gemini API Key
In PowerShell:
```powershell
$env:GEMINI_API_KEY="your-actual-gemini-api-key"
```

Or in Command Prompt (`cmd`):
```cmd
set GEMINI_API_KEY=your-actual-gemini-api-key
```

### 2. Run the Application
```powershell
mvn spring-boot:run
```

The application will start on port **8080**:
- 🌐 **Web Chat UI**: [http://localhost:8080/](http://localhost:8080/)
- 📖 **Swagger API Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 📄 **OpenAPI Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 💬 Sample Prompts to Try

| Category | Sample Prompt | Tool Used |
| :--- | :--- | :--- |
| **Knowledge Base (RAG)** | *"What is our refund policy for opened physical items?"* | `knowledgeBaseSearch` |
| **Knowledge Base (RAG)** | *"What is the employee home office equipment stipend?"* | `knowledgeBaseSearch` |
| **Live REST API** | *"What is the current live weather in Tokyo and Paris?"* | `weatherLookup` |
| **Enterprise CRM** | *"Track order #ORD-1042 in CRM and check its delivery status."* | `crmLookup` |
| **Enterprise CRM** | *"Lookup customer CUST-8812 and check their membership tier."* | `crmLookup` |
| **Dynamic HTTP Call** | *"Fetch the latest quote from https://api.github.com/zen"* | `genericRestApi` |

---

## 📡 REST API Reference

### 1. Synchronous Chat
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the return policy for opened items?",
    "conversationId": "session-1"
  }'
```

### 2. Streaming Chat (SSE)
```bash
curl -N "http://localhost:8080/api/chat/stream?message=What+is+the+weather+in+London%3F&conversationId=session-1"
```

### 3. Upload Document to Knowledge Base
```bash
curl -X POST http://localhost:8080/api/knowledge/upload \
  -F "file=@/path/to/document.pdf"
```

### 4. Direct Vector Search
```bash
curl "http://localhost:8080/api/knowledge/search?q=return+policy&topK=3"
```

---

## 📂 Project Structure

```
ai-agent/
├── pom.xml                               # Maven build configuration & Spring AI BOM
├── README.md                             # Project documentation
├── src/
│   ├── main/
│   │   ├── java/com/demo/agent/
│   │   │   ├── AiAgentApplication.java   # Spring Boot entry point
│   │   │   ├── config/                   # Agent & Swagger configuration
│   │   │   ├── controller/               # Chat & Knowledge REST controllers
│   │   │   ├── dto/                      # Request / response records
│   │   │   ├── memory/                   # PersistentChatMemory implementation
│   │   │   ├── rag/                      # Vector store, chunking & embeddings
│   │   │   └── tools/                    # Spring AI function calling tools
│   │   └── resources/
│   │       ├── application.yml           # App & Gemini configuration
│   │       ├── docs/                     # Pre-seeded RAG knowledge documents
│   │       └── static/                   # Modern Glassmorphic Web UI
│   │           ├── css/styles.css
│   │           ├── js/app.js
│   │           └── index.html
│   └── test/                             # Automated unit & integration tests
```
