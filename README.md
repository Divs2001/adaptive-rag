# adaptive-rag

Adaptive RAG pipeline in Java / Spring Boot. Each query is classified first, then routed to
document search (Qdrant), direct LLM generation, or live web search (Tavily).
See the project plan for the full design.

## Stack
Spring Boot 4 · Spring AI 2 · Ollama (`llama3.1:8b`, `nomic-embed-text`) · Qdrant · MongoDB · Java 21

## Local setup
```bash
# 1. Local models
brew install ollama
ollama pull llama3.1:8b
ollama pull nomic-embed-text

# 2. Infra containers (Rancher Desktop)
docker compose up -d      # Qdrant :6333/:6334, MongoDB :27017

# 3. Run
mvn spring-boot:run
```

## Package layout
`controller/` REST endpoints · `service/` router + handlers · `model/` DTOs/enums ·
`repository/` Mongo chat history · `config/` Ollama/Qdrant config
