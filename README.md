# 🧠 MaxKB4j — Enterprise Intelligent Q&A System: Out-of-the-Box RAG + LLM Workflow Engine

> **MaxKB4j = Max Knowledge Brain for Java**
> An out-of-the-box, model-agnostic **RAG (Retrieval-Augmented Generation) + LLM workflow engine** built on **Java 21 + Spring Boot 3 (Virtual Threads)**, designed for enterprise-grade intelligent Q&A systems.
> Use cases: intelligent customer service, enterprise knowledge bases, data analysis, academic research, and education.

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/badge/License-GPLv3-blue" alt="GPLv3"></a>
  <a href="https://github.com/taishan666/MaxKB4j/actions/workflows/ci.yml"><img src="https://github.com/taishan666/MaxKB4j/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21%2B-green" alt="Java21plus"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="SpringBoot3"></a>
  <a href="https://github.com/langchain4j/langchain4j"><img src="https://img.shields.io/badge/LangChain4j-1.x-green" alt="LangChain4j"></a>
  <a href="https://gitee.com/taisan/MaxKB4j"><img src="https://img.shields.io/gitee/stars/taisan/MaxKB4j?style=social&label=Gitee%20Stars" alt="GiteeStars"></a>
  <a href="https://gitee.com/taisan/MaxKB4j/commits/master"><img src="https://img.shields.io/gitee/last-commit/taisan/MaxKB4j" alt="LastCommit"></a><br/>
  [<a href="/README_CN.md">中文（简体）</a>] | [<a href="/README.md">English</a>]
</p>

<p align="center">
  🚀 <a href="#quick-start">Quick Start</a> · 🌐 <a href="http://43.143.235.194:8080/">Live Demo</a> (demo / demo@123456) · 📄 <a href="docs/MaxKB4j-商业价值白皮书.md">Whitepaper</a> · 🧪 <a href="docs/MaxKB4j-回归测试报告.md">Regression Report</a> · 🗒️ <a href="CHANGELOG.md">Changelog</a> · 💖 <a href="#support--sponsorship">Support Us</a>
</p>

---

## Why Choose MaxKB4j?

Enterprises face four common challenges when adopting LLM applications, and MaxKB4j answers each one:

| Pain Point | MaxKB4j Answer |
| :--- | :--- |
| **Complex integration** — mainstream platforms rely on Python / TS ecosystems, making adoption costly for Java teams | **Pure Java native** — built on the Spring Boot 3 stack; existing Java engineers can extend it directly with zero cross-language cost |
| **Serious hallucinations** — generic LLMs know nothing about internal enterprise data | **Production-grade RAG** — document parsing → chunking → vectorization → hybrid retrieval → Reranker re-ranking, with traceable answers |
| **Concurrency bottlenecks** — traditional architectures cannot sustain high-concurrency Q&A | **Virtual Threads + reactive architecture** — thousands of concurrent requests per node with lower resource usage |
| **Single-function Q&A** — no way to orchestrate complex business processes | **Visual workflow + Multi-Agent** — 30+ node types covering complex business scenarios |

## ✨ Key Capabilities

| Capability | Description |
| :--- | :--- |
| 🔍 Knowledge Base Q&A | Upload PDF / Word / TXT / Markdown files or crawl web pages; automatic chunking → vectorization → storage → RAG pipeline, significantly reducing hallucinations |
| 🧠 Advanced RAG / AgenticRAG | Vector, full-text, and hybrid multi-route retrieval + Reranker re-ranking; agents dynamically decide retrieval paths with intent recognition and conditional branches, supporting multi-hop Q&A |
| ⚙️ Visual Workflow | Low-code orchestration with 30+ node types: conditional branches, loops, variable aggregation, NL2SQL, forms, HTTP requests, MCP, etc.; multi-turn and long-term memory |
| 🤝 Multi-Agent Collaboration | Multiple role-specific agents (data analyst, code reviewer, customer service agent…) work in parallel or sequence; tasks are decomposed, dispatched, and aggregated automatically via a shared memory bus |
| ⏰ Triggers | Cron scheduled tasks + Webhook event callbacks for unattended automation (daily report generation, CRM-lead-triggered persona analysis, etc.) |
| 🌐 Model-Agnostic | Private models via Ollama / Xorbits Inference / LocalAI; public models: Qwen, DeepSeek, Doubao, Hunyuan, GLM, Kimi, GPT, Claude, Gemini, and more |
| 🧩 Seamless Integration | RESTful API, iframe / Web SDK embedding, OpenAI-compatible chat API, and stream_http MCP agent integration — connect within 5 minutes |
| 🎙️ Multimodal | ASR speech recognition, TTS speech synthesis, OCR image recognition, Stable Diffusion image generation |
| 🔒 Security & Permissions | Fine-grained permissions (application / knowledge base / tool / model) based on Sa-Token; audit logs; groovy-sandbox for safe script execution |
| 🌱 Ecosystem Extensions | Dozens of pre-built agent templates (customer service assistant, data analyst, code mentor…); plugin marketplace: MySQL / PostgreSQL / MongoDB connectors, Feishu / DingTalk / WeCom integrations, web search tools |

## 📊 How MaxKB4j Compares

| Capability | **MaxKB4j** | Dify | MaxKB | FastGPT | RAGFlow |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Backend stack | **Java 21 + Spring Boot 3** | Python + TS | Python | TypeScript | Python |
| Zero language-switch for Java teams | ✅ Native | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| Virtual-Thread high concurrency | ✅ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| Visual workflow + Multi-Agent | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| Triggers (Cron / Webhook) | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| Multimodal (ASR / TTS / OCR) | ✅ | ⚠️ | ✅ | ⚠️ | ✅ |
| MCP protocol / OpenAI-compatible API | ✅ | ✅ | ✅ | ✅ | ✅ |

> Note: this table is a capability-orientation comparison for selection reference. Evaluate with your own scenarios via the live demo below.

## 🚀 Quick Start

### Requirements
- Java 21+
- PostgreSQL 12+ (with pgvector extension enabled)
- MongoDB 6.0+ (full-text search and file storage)

### Option 1: Docker Compose (recommended)
```bash
docker-compose up -d
```
Then open `http://localhost:8080/admin/login` (default `admin` / `tarzan@123456`). The database is initialized automatically on first startup.

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
Interactive installers in `deploy/` handle environment checks, image pulling / building, and docker-compose orchestration (Linux / macOS / Windows):

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
Profiles: `maxkb4j-start/src/main/resources/application-{dev,prod,test}.yml`, switch with `--spring.profiles.active=dev`.

### Deploy to cloud platforms
Supports one-click deployment to [Sealos](https://blog.csdn.net/weixin_40986713/article/details/156026021) (overseas servers, no proxy needed, auto-scaling).

## 🌐 Live Demo & UI

- Live demo: http://43.143.235.194:8080/ (account `demo` / password `demo@123456`, normal-user permissions)
- Local default admin: `admin` / `tarzan@123456`

<img src="image/maxkb4j.gif" alt="MaxKB4jUI" />

## 🛠 Tech Stack & Project Structure

| Category | Tech |
| :--- | :--- |
| Backend | Java 21, Spring Boot 3, Virtual Threads, Sa-Token |
| AI framework | LangChain4j 1.x, Docling document parsing |
| Storage | PostgreSQL 15 + pgvector, MongoDB 6.0+, Caffeine cache |
| Frontend | Vue 3, Node.js v20.16.0 |
| Script sandbox | groovy-sandbox |

```
MaxKB4j/
├── maxkb4j-common / maxkb4j-core     # Utilities, core abstractions, domain models
├── maxkb4j-service/                  # Business implementation: application / chat / knowledge / model / oss / system / tool / trigger / workflow
├── maxkb4j-service-api/              # Public contracts: -api modules (DTOs / VOs)
├── maxkb4j-start/                    # Spring Boot entry point, config, packaging
└── deploy/                           # One-click install scripts (install.sh / install.bat)
```

> Dependency direction: `start` → `service` → `service-api` → `core` → `common`. Public contracts live in `-api` modules; implementations live in `service`.

## 📄 Documentation & Resources

- 📄 [Business Value Whitepaper](docs/MaxKB4j-商业价值白皮书.md) — value and selection analysis for enterprise decision makers
- 🧪 [Regression Test Report](docs/MaxKB4j-回归测试报告.md) — regression verification results of core features
- 📐 [Coding Conventions](docs/编码约定.md)
- 🗒️ [Changelog](CHANGELOG.md) — latest release: v2.9.0 (2026-06-17)

## 🤝 Community & Contributing

| Item | Details |
| :--- | :--- |
| Feedback & requests | Report bugs, suggestions, or feature requests via [Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues) |
| Contribution flow | Fork → create a branch → push to the DEV branch → open a Pull Request |
| Coding standards | Follow Alibaba Java coding conventions; include unit tests and update docs |
| Community chat | Add the author on WeChat (`vxhqqh`) to join the core community group |

## 💖 Support & Sponsorship

> MaxKB4j is maintained by an individual developer and community members, with no commercial company backing.
> Every contribution goes directly to: **cloud server costs, model token testing, bug fixes, and new feature development**, with usage reported regularly in the community group.

| Tier | Amount | Benefits | Best for |
| :---: | :---: | :--- | :--- |
| ☕ Coffee | ¥10 | Author WeChat `vxhqqh`, core community group, priority update notifications | Individual developers who appreciate the project |
| 📚 Learning Member | ¥99 | All Coffee benefits + free access to the [Knowledge Planet](https://wx.zsxq.com/group/28882525858841) + priority answers inside the planet | Developers who want to learn in depth |
| 🏢 Enterprise Partner | ¥799 | All Learning Member benefits + frontend source code (one-time) + deployment / post-sales support | Enterprise users / production deployments |
| 👑 Strategic Partner | ¥1399 | All Enterprise Partner benefits + 6-month frontend source upgrades + enterprise logo on the sponsor wall | Deep partners |

**How to sponsor**
1. Pick a tier and scan the Alipay / WeChat QR codes below to pay (please note your nickname).
2. Add the author on WeChat (`vxhqqh`) and send the payment screenshot to activate your benefits instantly.

> 💡 Please contact the author after payment so we can identify your sponsorship and grant benefits.

<table>
  <tr>
    <th align="center">Alipay QR</th>
    <th align="center">WeChat QR</th>
  </tr>
  <tr>
    <td align="center"><img src="image/zfb_skm.png" alt="AlipayQR" /></td>
    <td align="center"><img src="image/wx_zsm.png" alt="WeChatQR" /></td>
  </tr>
</table>

**🏅 Sponsor Wall** (sorted by sponsorship time; enterprise logos / nicknames are displayed long-term)
> 🎯 Join us — your enterprise logo / nickname will be showcased here with ongoing community exposure.

## 🏢 Enterprise Services

Need **private deployment, custom development, team training, or SLA-backed support**? Custom plans and quotes are available:
- Private / intranet deployment
- Scenario-specific development and system integration
- Team training and knowledge transfer
- Technical support and SLA services

Contact the author on WeChat (`vxhqqh`) for a tailored proposal and quote.

## 📜 License

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

This project is licensed under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) (“License”). You may not use the files of this project except in compliance with the License. Software distributed under the License is distributed on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

---

✅ **MaxKB4j — build high-performance, stable agent workflows and RAG knowledge base solutions with ease**
