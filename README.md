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

| Pain Point                                                                                                           | MaxKB4j Answer                                                                                                                          |
|:---------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------|
| **Complex integration** — mainstream platforms rely on Python / TS ecosystems, making adoption costly for Java teams | **Pure Java native** — built on the Spring Boot 3 stack; existing Java engineers can extend it directly with zero cross-language cost   |
| **Serious hallucinations** — generic LLMs know nothing about internal enterprise data                                | **Production-grade RAG** — document parsing → chunking → vectorization → hybrid retrieval → Reranker re-ranking, with traceable answers |
| **Concurrency bottlenecks** — traditional architectures cannot sustain high-concurrency Q&A                          | **Virtual Threads + reactive architecture** — thousands of concurrent requests per node with lower resource usage                       |
| **Single-function Q&A** — no way to orchestrate complex business processes                                           | **Visual workflow + Multi-Agent** — 30+ node types covering complex business scenarios                                                  |

## ✨ Key Capabilities

| Capability                   | Description                                                                                                                                                                                                         |
|:-----------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🔍 Knowledge Base Q&A        | Upload PDF / Word / TXT / Markdown files or crawl web pages; automatic chunking → vectorization → storage → RAG pipeline, significantly reducing hallucinations                                                     |
| 🧠 Advanced RAG / AgenticRAG | Vector, full-text, and hybrid multi-route retrieval + Reranker re-ranking; agents dynamically decide retrieval paths with intent recognition and conditional branches, supporting multi-hop Q&A                     |
| ⚙️ Visual Workflow           | Low-code orchestration with 30+ node types: conditional branches, loops, variable aggregation, NL2SQL, forms, HTTP requests, MCP, etc.; multi-turn and long-term memory                                             |
| 🤝 Multi-Agent Collaboration | Multiple role-specific agents (data analyst, code reviewer, customer service agent…) work in parallel or sequence; tasks are decomposed, dispatched, and aggregated automatically via a shared memory bus           |
| ⏰ Triggers                   | Cron scheduled tasks + Webhook event callbacks for unattended automation (daily report generation, CRM-lead-triggered persona analysis, etc.)                                                                       |
| 🌐 Model-Agnostic            | Private models via Ollama / Xorbits Inference / LocalAI; public models: Qwen, DeepSeek, Doubao, Hunyuan, GLM, Kimi, GPT, Claude, Gemini, and more                                                                   |
| 🧩 Seamless Integration      | RESTful API, iframe / Web SDK embedding, OpenAI-compatible chat API, and stream_http MCP agent integration — connect within 5 minutes                                                                               |
| 🎙️ Multimodal               | ASR speech recognition, TTS speech synthesis, OCR image recognition, Stable Diffusion image generation                                                                                                              |
| 🔒 Security & Permissions    | Fine-grained permissions (application / knowledge base / tool / model) based on Sa-Token; audit logs; groovy-sandbox for safe script execution                                                                      |
| 🌱 Ecosystem Extensions      | Dozens of pre-built agent templates (customer service assistant, data analyst, code mentor…); plugin marketplace: MySQL / PostgreSQL / MongoDB connectors, Feishu / DingTalk / WeCom integrations, web search tools |

## 📊 How MaxKB4j Compares

| Capability                           |         **MaxKB4j**         |    Dify     | MaxKB  |  FastGPT   | RAGFlow |
|:-------------------------------------|:---------------------------:|:-----------:|:------:|:----------:|:-------:|
| Backend stack                        | **Java 21 + Spring Boot 3** | Python + TS | Python | TypeScript | Python  |
| Zero language-switch for Java teams  |          ✅ Native           |     ⚠️      |   ⚠️   |     ⚠️     |   ⚠️    |
| Virtual-Thread high concurrency      |              ✅              |     ⚠️      |   ⚠️   |     ⚠️     |   ⚠️    |
| Visual workflow + Multi-Agent        |              ✅              |      ✅      |   ⚠️   |     ⚠️     |   ⚠️    |
| Triggers (Cron / Webhook)            |              ✅              |      ✅      |   ⚠️   |     ⚠️     |   ⚠️    |
| Multimodal (ASR / TTS / OCR)         |              ✅              |     ⚠️      |   ✅    |     ⚠️     |    ✅    |
| MCP protocol / OpenAI-compatible API |              ✅              |      ✅      |   ✅    |     ✅      |    ✅    |

> Note: this table is a capability-orientation comparison for selection reference. Evaluate with your own scenarios via
> the live demo below.

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

Profiles: `maxkb4j-start/src/main/resources/application-{dev,prod,test}.yml`, switch with
`--spring.profiles.active=dev`.

### Deploy to cloud platforms

Supports one-click deployment to [Sealos](https://blog.csdn.net/weixin_40986713/article/details/156026021) (overseas
servers, no proxy needed, auto-scaling).

> 🎉 **Deployed successfully?**
> If MaxKB4j saved your team development time, consider [buying the author a coffee ☕](#support--sponsorship) — a coffee
> unlocks the author's direct WeChat line and the core community group, where deployment questions get answered fast.

## 🌐 Live Demo & UI

- Live demo: http://43.143.235.194:8080/ (account `demo` / password `demo@123456`, normal-user permissions)
- Local default admin: `admin` / `tarzan@123456`

<img src="image/maxkb4j.gif" alt="MaxKB4jUI" />

> 💡 **Spend 3 minutes on the demo first, then decide** — seeing is believing.

## 🛠 Tech Stack & Project Structure

| Category       | Tech                                                   |
|:---------------|:-------------------------------------------------------|
| Backend        | Java 21, Spring Boot 3, Virtual Threads, Sa-Token      |
| AI framework   | LangChain4j 1.x, Docling document parsing              |
| Storage        | PostgreSQL 15 + pgvector, MongoDB 6.0+, Caffeine cache |
| Frontend       | Vue 3, Node.js v20.16.0                                |
| Script sandbox | groovy-sandbox                                         |

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

## 📄 Documentation & Resources

- 📄 [Business Value Whitepaper](docs/MaxKB4j-商业价值白皮书.md) — value and selection analysis for enterprise decision
  makers
- 🧪 [Regression Test Report](docs/MaxKB4j-回归测试报告.md) — regression verification results of core features
- 📐 [Coding Conventions](docs/编码约定.md)
- 🗒️ [Changelog](CHANGELOG.md) — latest release: v2.9.0 (2026-06-17)

## 🤝 Contributing

| Item                | Details                                                                                                   |
|:--------------------|:----------------------------------------------------------------------------------------------------------|
| Feedback & requests | Report bugs, suggestions, or feature requests via [Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues) |
| Contribution flow   | Fork → create a branch → push to the DEV branch → open a Pull Request                                     |
| Coding standards    | Follow Alibaba Java coding conventions; include unit tests and update docs                                |

## 💖 Support & Sponsorship

> **MaxKB4j has no commercial backing and no funding** — it is built line by line by an independent developer in his
> spare time.
> Every contribution goes directly to real costs:
>
> - 🖥 **Cloud servers for the live demo** — running 24/7, free for everyone to try
> - 🤖 **Model token consumption** — every regression test costs real money
> - 🧪 **Bug fixes & new feature development** — caffeine for late-night coding
>
> Usage is reported regularly in the community group, fully transparent.
>
> ⚠️ The author's time is limited: **direct WeChat access and the core community group are reserved for sponsors** (from
> ¥10) — this protects both the author's focus and the signal quality of the group.

### Sponsorship Tiers

|         Tier          | Amount | Benefits                                                                                                                                   | Best for                                                 |
|:---------------------:|:------:|:-------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------|
|       ☕ Coffee        |  ¥10   | Direct WeChat line to the author + core community group + priority update notifications                                                    | Anyone who appreciates the project                       |
|  📚 Learning Member   |  ¥99   | All Coffee benefits + free access to the [Knowledge Planet](https://wx.zsxq.com/group/28882525858841) + priority answers inside the planet | Developers who want to master RAG / workflow in practice |
| 🏢 Enterprise Partner |  ¥799  | All Learning Member benefits + frontend source code (one-time) + deployment / post-sales support                                           | Teams going to production                                |
| 👑 Strategic Partner  | ¥1399  | All Enterprise Partner benefits + 6-month frontend source upgrades + your logo on the sponsor wall                                         | Long-term partners growing together                      |

> 💡 **Every tier includes the author's direct WeChat line + the core community group** — unlocked from the ¥10 Coffee
> tier.

### How to Sponsor (3 steps)

<table>
  <tr>
    <th align="center">Step 1 · Add the author on WeChat</th>
    <th align="center">Step 2 · Pay via Alipay</th>
    <th align="center">Step 2 · Pay via WeChat</th>
  </tr>
  <tr>
    <td align="center"><img src="image/wx.jpg" width="200" alt="Author WeChat QR"/></td>
    <td align="center"><img src="image/zfb_skm.png" width="200" alt="Alipay QR"/></td>
    <td align="center"><img src="image/wx_zsm.png" width="200" alt="WeChat QR"/></td>
  </tr>
  <tr>
    <td align="center">Note "MaxKB4j"</td>
    <td align="center" colspan="2">Step 3 · Send the payment screenshot to the author (with your nickname) — benefits activate immediately</td>
  </tr>
</table>

### 🏅 Sponsor Wall

> 🎯 Join us — your enterprise logo / nickname will be showcased here by sponsorship order, with ongoing community
> exposure.

## 🏢 Enterprise Services

Need **private deployment, custom development, team training, or SLA-backed support**? Custom plans and quotes are
available:

- Private / intranet deployment
- Scenario-specific development and system integration
- Team training and knowledge transfer
- Technical support and SLA services

Contact the author on WeChat (`vxhqqh`) for a tailored proposal and quote.

## ✍️ A Note from the Author

Hi, I'm **TARZAN (泰山)**, the author of MaxKB4j.

This project started from a simple belief: **the Java world deserves a native, production-grade AI application platform
**. With no company and no funding behind it, all the code, docs, tests, and deployment scripts were written in my
evenings and weekends — and I personally answer questions in the community group whenever I can.

If this project helps you, here are two ways to support it:

- ⭐ **Give it a Star** — 10 seconds of your time helps more Java developers discover it
- 💖 **[Buy me a coffee](#support--sponsorship)** (from ¥10) — unlocks the author's direct WeChat line and the core
  community group; your sponsorship becomes servers, tokens, and caffeine that keep this project moving forward

Open source is hard; every bit of kindness counts. 🤝

## 📜 License

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

This project is licensed under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) (“License”). You may not use
the files of this project except in compliance with the License. Software distributed under the License is distributed
on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

---

✅ **MaxKB4j — build high-performance, stable agent workflows and RAG knowledge base solutions with ease**
