<p align="center">
  <img src="image/logo.png" width="130" alt="MaxKB4j Logo"/>
</p>

<h1 align="center">🧠 MaxKB4j</h1>

<p align="center">
  <b>Max Knowledge Brain for Java</b> — an enterprise-grade AI brain for Java teams, with zero friction<br/>
  An out-of-the-box, model-agnostic <b>RAG + LLM workflow engine</b> built on <b>Java 21 + Spring Boot 3 (Virtual Threads)</b><br/>
  <sub>Intelligent customer service · Enterprise knowledge bases · Data analysis · Research & education</sub>
</p>

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/badge/License-GPLv3-blue" alt="GPLv3"></a>
  <a href="https://github.com/taishan666/MaxKB4j/actions/workflows/ci.yml"><img src="https://github.com/taishan666/MaxKB4j/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21%2B-green" alt="Java21plus"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="SpringBoot3"></a>
  <a href="https://github.com/langchain4j/langchain4j"><img src="https://img.shields.io/badge/LangChain4j-1.x-green" alt="LangChain4j"></a>
  <a href="https://gitee.com/taisan/MaxKB4j"><img src="https://gitee.com/taisan/MaxKB4j/badge/star.svg" alt="GiteeStars"></a>
  <a href="https://gitee.com/taisan/MaxKB4j/members"><img src="https://gitee.com/taisan/MaxKB4j/badge/fork.svg" alt="GiteeForks"></a>
  <a href="https://gitee.com/taisan/MaxKB4j/issues"><img src="https://img.shields.io/badge/Issues-Welcome-orange" alt="IssuesWelcome"></a><br/>
  [<a href="README_CN.md">中文（简体）</a>] | [<a href="README.md">English</a>]
</p>

<p align="center">
  💬 Found a problem or have a suggestion? Feel free to open an <a href="https://gitee.com/taisan/MaxKB4j/issues">Issue</a>
</p>

<p align="center">
  🚀 <a href="#quick-start">Quick Start</a> · 🌐 <a href="http://43.143.235.194:8080/">Live Demo</a> (demo / demo@123456) · 🗒️ <a href="CHANGELOG.md">Changelog</a> · 📄 <a href="#documentation">Docs</a> · 🤝 <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

## Why Choose MaxKB4j?

Enterprises face four common challenges when adopting LLM applications, and MaxKB4j answers each one:

| Pain Point                                                                                                           | MaxKB4j Answer                                                                                                                          |
|:---------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------|
| **Complex integration** — mainstream platforms rely on Python / TS ecosystems, making adoption costly for Java teams | **Pure Java native** — built on the Spring Boot 3 stack; existing Java engineers can extend it directly with zero cross-language cost   |
| **Serious hallucinations** — generic LLMs know nothing about internal enterprise data                                | **Production-grade RAG** — document parsing → chunking → vectorization → hybrid retrieval → Reranker re-ranking, with traceable answers |
| **Concurrency bottlenecks** — traditional architectures cannot sustain high-concurrency Q&A                          | **Virtual Threads + reactive architecture** — thousands of concurrent requests per node with lower resource usage                       |
| **Single-function Q&A** — no way to orchestrate complex business processes                                           | **Visual workflow + Multi-Agent** — 30+ node types covering complex business scenarios                                                  |

## ✨ Key Capabilities

| Capability                   | Description                                                                                                                                                                                                         |
|:-----------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🔍 Knowledge Base Q&A        | Upload PDF / Word / TXT / Markdown files or crawl web pages; automatic chunking → vectorization → storage → RAG pipeline, significantly reducing hallucinations; in-document images are fused into vectors for image-text hybrid retrieval                              |
| 🧠 Advanced RAG / AgenticRAG | Vector, full-text, and hybrid multi-route retrieval + Reranker re-ranking; agents dynamically decide retrieval paths with intent recognition and conditional branches, supporting multi-hop Q&A                     |
| ⚙️ Visual Workflow           | Low-code orchestration with 30+ node types: conditional branches, loops, variable aggregation, NL2SQL, forms, HTTP requests, MCP, etc.; multi-turn and long-term memory                                             |
| 🤝 Multi-Agent Collaboration | Multiple role-specific agents (data analyst, code reviewer, customer service agent…) work in parallel or sequence; tasks are decomposed, dispatched, and aggregated automatically via a shared memory bus           |
| ⏰ Triggers                   | Cron scheduled tasks + Webhook event callbacks for unattended automation (daily report generation, CRM-lead-triggered persona analysis, etc.)                                                                       |
| 🌐 Model-Agnostic            | Private models via Ollama / Xorbits Inference / LocalAI; public models: Qwen, DeepSeek, Doubao, Hunyuan, GLM, Kimi, GPT, Claude, Gemini, and more                                                                   |
| 🧩 Seamless Integration      | RESTful API, iframe / Web SDK embedding, OpenAI-compatible chat API, and stream_http MCP agent integration — connect within 5 minutes                                                                               |
| 🎙️ Multimodal               | **Multimodal fusion embedding** — text and images in documents are fused into unified vectors, enabling text-to-image and image-to-text retrieval (Aliyun Bailian / Volcengine Ark vision embedding models); ASR speech recognition, TTS speech synthesis, OCR image recognition, Stable Diffusion image generation |
| 🔒 Security & Permissions    | Fine-grained permissions (application / knowledge base / tool / model) based on Sa-Token; audit logs; groovy-sandbox for safe script execution                                                                      |
| 🌱 Ecosystem Extensions      | Dozens of pre-built agent templates (customer service assistant, data analyst, code mentor…); plugin marketplace: MySQL / PostgreSQL / MongoDB connectors, Feishu / DingTalk / WeCom integrations, web search tools |

## 📊 How MaxKB4j Compares

| Capability                              |         **MaxKB4j**         |    Dify     | MaxKB  |  FastGPT   | RAGFlow |
|:----------------------------------------|:---------------------------:|:-----------:|:------:|:----------:|:-------:|
| Backend stack                           | **Java 21 + Spring Boot 3** | Python + TS | Python | TypeScript | Python  |
| Visual workflow                         |              ✅              |      ✅      |   ✅    |     ✅      |    ✅    |
| Multi-Agent collaboration               |              ✅              |      ✅      |   —    |     —      |    —    |
| Triggers (Cron / Webhook)               |              ✅              |      ✅      |   —    |     —      |    —    |
| Full built-in multimodal (ASR/TTS/OCR)  |              ✅              |      —      |   ✅    |     —      |    ✅    |
| Text + image fusion embedding           |              ✅              |      —      |   —    |     —      |    —    |
| MCP protocol / OpenAI-compatible API    |              ✅              |      ✅      |   ✅    |     ✅      |    ✅    |
| Virtual-Thread concurrency              |              ✅              |      —      |   —    |     —      |    —    |

> ✅ = supported · — = not supported / requires combining other solutions. Compiled from each project's public docs
> (as of 2026-10); features evolve quickly, so verify against the official docs of each project and evaluate with
> your own scenarios via the live demo.

## 🚀 Quick Start

### Requirements

- Java 21+
- PostgreSQL 12+ (with pgvector extension enabled)
- MongoDB 6.0+ (full-text search and file storage)

### Option 1: Docker Compose (recommended)

```bash
docker-compose up -d
```

Then open `http://localhost:8080/admin/login` (default `admin` / `tarzan@123456`). The database is initialized
automatically on first startup.

### Option 2: Docker single container

```bash
docker run --name maxkb4j -d --restart always -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin \
  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j
```

### Option 3: One-click install script

Interactive installers in `deploy/` handle environment checks, image pulling / building, and docker-compose
orchestration (Linux / macOS / Windows):

```bash
# Linux / macOS
chmod +x deploy/install.sh
./deploy/install.sh
# Windows
deploy\install.bat
```

### Option 4: Build from source

```bash
mvn clean package -DskipTests
java -jar maxkb4j-start/target/maxkb4j-start.jar
```

Profiles: `maxkb4j-start/src/main/resources/application-{dev,prod}.yml`, switch with
`--spring.profiles.active=dev`.

### Deploy to cloud platforms

Supports one-click deployment to [Sealos](https://blog.csdn.net/weixin_40986713/article/details/156026021) (overseas
servers, no proxy needed, auto-scaling).

## 🌐 Live Demo

- Live demo: http://43.143.235.194:8080/ (account `demo` / password `demo@123456`, normal-user permissions)
- Local default admin: `admin` / `tarzan@123456`

<img src="image/maxkb4j.gif" alt="MaxKB4jUI" />

## 🛠 Tech Stack & Project Structure

| Category       | Tech                                                    |
|:---------------|:--------------------------------------------------------|
| Backend        | Java 21, Spring Boot 3, Virtual Threads, Sa-Token       |
| AI framework   | LangChain4j 1.x, Docling document parsing, Volcengine Ark SDK |
| Storage        | PostgreSQL 15 + pgvector, MongoDB 6.0+, Caffeine cache  |
| Frontend       | Element Plus, Vue 3, Node.js v20.16.0                    |
| Script sandbox | groovy-sandbox                                          |

```
MaxKB4j/
├── maxkb4j-common / maxkb4j-core     # Utilities, core abstractions, domain models
├── maxkb4j-service/                  # Business implementation: application / chat / knowledge / model / oss / system / tool / trigger / workflow
├── maxkb4j-service-api/              # Public contracts: -api modules (DTOs / VOs)
├── maxkb4j-start/                    # Spring Boot entry point, config, packaging
└── deploy/                           # One-click install scripts (install.sh / install.bat)
```

> Dependency direction: `start` → `service` → `service-api` → `core` → `common`. Public contracts live in `-api`
> modules; implementations live in `service`.

## 📄 Documentation

> The documents below are written in Chinese (中文). English translations are on the roadmap — contributions welcome.

- 🗒️ [Changelog](CHANGELOG.md) — latest release: **v2.10.6** (2026-10-09)
- 📄 [Business Value Whitepaper](docs/MaxKB4j-商业价值白皮书.md)（中文）— value and selection analysis for enterprise decision makers
- 🧪 [Regression Test Report](docs/MaxKB4j-回归测试报告.md)（中文）— regression verification results of core features
- 📐 [Coding Conventions](docs/编码约定.md)（中文）

## 🤝 Contributing

Issues and pull requests are welcome on both [Gitee](https://gitee.com/taisan/MaxKB4j/issues) (primary) and
[GitHub](https://github.com/taishan666/MaxKB4j/issues). See [CONTRIBUTING.md](CONTRIBUTING.md) for the development
setup, branch model and coding standards, and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community norms.

## 📜 License

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

MaxKB4j adopts an **open-core** licensing model:

- **Backend (this repository)** — licensed under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html). You may
  use, study, modify and redistribute it under the license terms.
- **Web console** — distributed as prebuilt assets bundled in the official Docker image and source tree. Its source
  code is not part of this repository and is licensed separately (available via the
  [commercial tiers](#-sponsorship)); see the “支持与赞助” section of the [Chinese README](README_CN.md) for details.

Software distributed under the License is distributed on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.

## 💖 Sponsorship

MaxKB4j is built by an independent developer (TARZAN) in his spare time — no company, no funding. The live-demo
server and the model tokens burned by every regression test are paid out of pocket.

If the project helps you:

- ⭐ **Star this repo** — 10 seconds of your time helps more Java developers discover it
- 💖 **Sponsor** (from ¥10, WeChat / Alipay) — unlocks the author's direct contact and the core community group;
  tiers and payment details are in the “支持与赞助” section of the [Chinese README](README_CN.md)

Commercial services (private deployment, custom development, team training, SLA support) are available — contact the
author on WeChat (`vxhqqh`).

---

✅ **MaxKB4j — build high-performance, stable agent workflows and RAG knowledge base solutions with ease**
