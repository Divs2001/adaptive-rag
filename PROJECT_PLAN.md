# Adaptive RAG — Java/Spring Boot Rebuild

A from-scratch Java reimplementation of an agentic, adaptive RAG system — originally built in Python (LangGraph + FastAPI + Streamlit), rebuilt here in Spring Boot for resume/interview purposes.

---

## 1. What this project is

A RAG (Retrieval-Augmented Generation) system that does **not** always answer from a vector database. Before retrieving anything, it classifies the incoming query and routes it to one of three sources:

1. **Document search** (Qdrant vector DB) — for questions answerable from uploaded documents
2. **Direct LLM generation** — for general-knowledge questions that don't need retrieval at all
3. **Live web search** — for questions needing current/external info not in the documents

This routing decision — made *before* retrieval — is the core "adaptive" idea, and it's the main thing that differentiates this from a basic RAG project where every query goes through the same fixed pipeline.

### Why this matters (the problem it solves)
- Basic RAG wastes a vector search on questions it can't help with (e.g. "what's 2+2").
- Basic RAG also fails or hallucinates on questions outside the uploaded documents, since it has no fallback source.
- Routing + a relevance-grading step reduces unnecessary retrieval/LLM calls (cost/latency) and reduces hallucination risk (quality).

### Practical use cases to cite in interviews
- Internal company knowledge bot (docs vs. general knowledge vs. "is the API down right now")
- Personal study/research assistant over your own notes, falling back to general knowledge or the web
- Developer assistant over internal docs + general programming knowledge
- Cost/latency optimization at scale — not every query needs expensive retrieval

### Personal framing for "why did you build this"
> I'd seen basic RAG examples where every question goes through vector search regardless of whether the answer is actually in the documents, and that breaks down fast — it either hallucinates from irrelevant chunks or fails on anything outside the uploaded material. I wanted to understand how to build a system that decides *how* to answer a question before committing to a retrieval strategy, and it was a good way to learn multi-step orchestration (routing, branching, state) instead of just a single linear LLM call.

---

## 2. Tech stack (100% free / local)

| Component | Technology |
|---|---|
| Backend framework | Spring Boot |
| AI orchestration | Spring AI |
| Chat LLM | **Ollama** — `llama3.1:8b` or `mistral:7b` (local, free, Metal-accelerated on Apple Silicon) |
| Embeddings | **Ollama** — `nomic-embed-text` |
| Vector DB | Qdrant (self-hosted via Rancher Desktop container) |
| Chat history DB | MongoDB (self-hosted via Rancher Desktop container) |
| Web search | Tavily (free tier, ~1,000 searches/month) |
| Document parsing | Apache PDFBox (for PDF text extraction) |
| Frontend | Minimal HTML form or React (kept simple — not the focus) |
| Containers | Rancher Desktop (free, open-source Docker Desktop alternative) |

**Hardware confirmed suitable:** MacBook, 24GB unified memory, Apple M5 chip — Ollama uses Metal acceleration, so local inference is fast, not CPU-bound.

**Total cost: $0** (Tavily free tier covers normal weekend testing volume).

---

## 3. Architecture

```
┌─────────────┐      ┌──────────────────┐
│  Frontend   │─────▶│  Spring Boot API  │
│ (HTML/React)│      │  POST /rag/query   │
└─────────────┘      │  POST /rag/documents/upload │
                      └────────┬─────────┘
                               │
                      ┌────────▼─────────┐
                      │ QueryRouterService│  ← classifies query via Ollama LLM call
                      └────────┬─────────┘
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
  ┌──────────────────┐ ┌───────────────┐ ┌──────────────────┐
  │ DocumentService    │ │ GenerationService│ │ WebSearchService │
  │ (Qdrant retrieval) │ │ (direct Ollama) │ │ (Tavily API)     │
  └──────────────────┘ └───────────────┘ └──────────────────┘
            │                  │                  │
            └──────────────────┼──────────────────┘
                               ▼
                      ┌──────────────────┐
                      │ Generation (Ollama)│  ← final answer, using gathered context
                      └────────┬─────────┘
                               ▼
                      ┌──────────────────┐
                      │ ChatHistoryRepo    │  ← persists to MongoDB, keyed by sessionId
                      └──────────────────┘
```

### Package structure
```
controller/   → RagController
service/      → QueryRouterService, DocumentService, GenerationService, WebSearchService
model/        → QueryRequest, RouteDecision (enum), ChatMessage
repository/   → ChatHistoryRepository (Mongo)
config/       → OllamaConfig, QdrantConfig
```

### Routing logic design
- Implemented manually (not via a library) using a **Strategy pattern** — `interface QueryHandler` with three implementations (`DocumentQueryHandler`, `GeneralQueryHandler`, `WebSearchQueryHandler`).
- Classification: send the query to the local LLM with a prompt like *"Respond with exactly one word: DOCUMENT, GENERAL, or WEB, based on the following query..."*, parse the response into a `RouteDecision` enum, dispatch to the matching handler.
- This is a deliberately stronger interview talking point than relying on a prebuilt routing library — it demonstrates understanding of the underlying design pattern.

---

## 4. Build plan (1 free day + weekend)

### Day 1 — Foundation + first working LLM call
- [ ] Install JDK 21, Maven/Gradle, IntelliJ, Rancher Desktop
- [ ] Install Ollama (`brew install ollama`); pull `llama3.1:8b` and `nomic-embed-text`
- [ ] Generate Spring Boot project (Spring Web, Spring AI Ollama starter, Spring Data MongoDB)
- [ ] Build `POST /rag/query` → direct Ollama call, no retrieval yet
- [ ] Start Qdrant + MongoDB containers via Rancher Desktop
- [ ] Sanity-test embeddings with a throwaway script
- [ ] Set up package structure (above)

**Checkpoint:** direct LLM Q&A works end-to-end; infra containers running.

### Day 2 — Document ingestion + real RAG
- [ ] Build `POST /rag/documents/upload` (PDFBox text extraction)
- [ ] Chunk text (fixed-size, ~500 words, slight overlap)
- [ ] Generate embeddings per chunk via Ollama; store in Qdrant (Spring AI `VectorStore`)
- [ ] Build retrieval: embed query → similarity search → top-k chunks
- [ ] Wire retrieval into generation (inject chunks into prompt)
- [ ] Test an out-of-scope question and observe the failure mode (useful for interview framing)

**Checkpoint:** basic (non-adaptive) RAG fully functional.

### Day 3 — Adaptive routing + memory + wrap-up
- [ ] Build `QueryRouterService` (LLM-based classification → `RouteDecision`)
- [ ] Wire router into controller; dispatch to the three `QueryHandler` strategies
- [ ] Integrate Tavily web search as the third source
- [ ] Add MongoDB-backed chat history, keyed by `sessionId`, pulled into prompt for multi-turn context
- [ ] Manually test one question per route (document / general / web) — **write these test cases down**, they're your answer to "how did you evaluate the routing?"
- [ ] Write README (architecture, setup instructions, screenshots)
- [ ] Minimal frontend (plain HTML form is fine)
- [ ] Push to GitHub

**Deferred to "future work"** (fine to mention as next steps, not needed for MVP): relevance grading layer, hallucination checking, polished React UI, full Docker Compose packaging, live deployment.

---

## 5. Key concepts to know cold for interviews

**Foundational:** LLM, prompt, token, hallucination
**RAG-specific:** RAG, embedding, vector database, chunking, semantic search, retriever
**Adaptive/agentic layer:** agentic AI, query routing/classification, LangGraph-style orchestration (node/edge/state — conceptual reference even though this Java build implements routing manually), relevance grading
**Infra:** REST API, Spring Boot, MongoDB, session ID, environment variables/API keys

**Priority if short on time:** RAG → embeddings → vector database → hallucination → agentic AI/query routing.

**Common interviewer follow-up to prepare for:** *"How did you evaluate whether the routing/grading was working well?"* — Answer: manually tested a set of questions known to belong to each category (document / general / web) and confirmed the router sent each down the correct path, plus checked ambiguous edge cases.

---

## 6. Resume bullet drafts

- Built an agentic RAG system using Spring AI, Spring Boot, and Qdrant that dynamically routes queries between document retrieval, web search, and direct LLM generation based on query classification, reducing unnecessary retrieval calls for simple queries.
- Designed query routing using the Strategy design pattern, with classification handled via a local LLM (Ollama) call rather than a prebuilt routing library.
- Implemented persistent multi-turn conversation memory using MongoDB, exposed via a Spring Boot REST API (`/rag/query`, `/rag/documents/upload`).
- Ran the entire stack locally and free of cost using Ollama for inference/embeddings and Rancher Desktop containers for Qdrant/MongoDB.
