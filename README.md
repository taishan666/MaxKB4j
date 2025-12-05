# 🧠 MaxKB4j — An Open-Source RAG Knowledge Base and LLM Workflow Platform Built with Java
> **MaxKB4j = Max Knowledge Base for Java**</br>
> A ready-to-use, secure, model-agnostic RAG (Retrieval-Augmented Generation) + LLM workflow engine, purpose-built for enterprise-grade intelligent Q&A systems.</br>
> Widely used in scenarios such as intelligent customer service, internal enterprise knowledge bases, data analysis, academic research, and education.

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html#license-text"><img src="https://img.shields.io/badge/License-GPL%20v3-blue" alt="License: GPL v3"></a>
  <a href=""><img src="https://img.shields.io/badge/Java-17+-green" alt="Java 17+"></a>
  <a href=""><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x"></a>    
  <a href=""><img src="https://img.shields.io/badge/Langchain4J-1.x-green" alt="Nodejs"></a><br/>
 [<a href="/README_CN.md">中文(简体)</a>] | [<a href="/README.md">English</a>] 
</p>

## ✨ Core Features
### 🔍 Out-of-the-Box Knowledge Base Q&A
Supports uploading local documents (PDF/Word/TXT/Markdown, etc.) or automatically crawling web content
Automatically handles: text chunking → vectorization → storage in a vector database → construction of the RAG pipeline
Significantly reduces LLM "hallucinations," enhancing answer accuracy and reliability
### 🌐 Model-Agnostic & Flexible Integration
Supports a wide range of mainstream large language models, including:
Local private models: DeepSeek-R1, Llama 3, Qwen 2, etc. (via Ollama / LM Studio / vLLM)
Chinese public models: Qwen (Tongyi Qianwen), Tencent HunYuan, ByteDance Doubao, Baidu Qianfan, Zhipu GLM, Kimi
Global public models: OpenAI (GPT), Anthropic (Claude), Google (Gemini)
Just configure an API key or local endpoint to seamlessly switch between models!
### ⚙️ Visual Workflow Orchestration
Built-in low-code AI workflow engine supporting conditional branching, function calling, and multi-turn conversation memory
Comes with a rich library of built-in functions (HTTP requests, database queries, time processing, regex extraction, etc.)
Ideal for complex business scenarios: customer support ticket generation, data report interpretation, internal policy Q&A, and more
### 🧩 Seamless Integration into Existing Systems
Provides RESTful APIs and frontend embedding components (iframe / Web SDK)
Add intelligent Q&A capabilities to your current system in just 5 minutes—no refactoring required
### 🤖 MCP Server Support (Model Context Protocol)
Implements the [MCP](https://modelcontextprotocol.io/) protocol, enabling AI to understand code context, project structure, and dependencies
Transforms your assistant from a simple chatbot into a true AI programming collaborator
### 🎙️ Multimodal Capabilities (Planned)
Already supports: Speech-to-Text (ASR), Text-to-Speech (TTS), Optical Character Recognition (OCR), and image generation (Stable Diffusion)
Video generation model support is under active development…

---

## 🚀 Quick Start
### 1. System Requirements
   Java 17+
   Maven or Gradle
   PostgreSQL 12+ (with pgvector extension enabled)
   MongoDB 6.0+ (optional, for full-text search)
### 2. Run the Application
   ```bash
   # Start the application
   java -jar MaxKB4j.jar
   ```
### 3. Deploy with Docker
   ```bash
   docker run --name maxkb4j -d --restart always -p 8080:8080 \
   -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j \
   -e SPRING_DATASOURCE_USERNAME=postgres \
   -e SPRING_DATASOURCE_PASSWORD=123456 \
   -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin \
   registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j
   ```
   Note: The first 8080 in -p 8080:8080 is the host port. The PostgreSQL and MongoDB connection parameters can be customized as needed.
### 4. Deploy with Docker Compose (Recommended)
   yaml
   See the sample docker-compose.yml in the project root
   docker-compose up -d
### 5. Access the Web UI
-   URL: http://localhost:8080/admin/login
-   Default username: admin
-   Default password: tarzan@123456
>   On first launch, the system will auto-initialize both PostgreSQL and MongoDB. Ensure required ports are available.

---
## 🛠 Tech Stack

|Category | Technologies                                      |
|------|---------------------------------------------------|
|Backend | Java 17, Spring Boot 3, Sa-Token (authentication) |
|AI Framework| LangChain4j                                       |
|Vector DB | PostgreSQL 15 + pgvector                          |
|Full-Text Search | MongoDB 5+                                        |
|Caching | Caffeine                                          |
|Frontend | Vue 3, Node.js v20.16.0                           |

---

## 📸 UI Preview
<img src="image/maxkb4j.gif" alt="MaxKB4j team" />

> For more screenshots, please refer to the project Wiki or try a live deployment.

## ❓ Questions & Suggestions

Feel free to submit Issues or PRs!
👉 [Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues)

## 💖 Support & Sponsorship

This project is maintained by an individual developer. Your support helps ensure continuous development!

|Donation Amount| Benefits|
|--------|------|
|¥20 |Add author on WeChat (vxhqqh) and join the discussion group (mention “donated”)|
|¥50 |Above benefits + free access to [Knowledge Planet🔥](https://wx.zsxq.com/group/28882525858841)|
|¥200 |Get V1 frontend source code|
|¥500 |Get full V2 source code (frontend + backend, including latest features)|
<table style="border-collapse: collapse; border: 1px solid black;">
<tr>
<th style="padding: 10px;"> <div align="center">Alipay QR Code</div></th>
<th style="padding: 10px;"> <div align="center">WeChat QR Code</div></th>
</tr>
<tr>
<td style="padding: 5px;background-color:#fff;"><img src="image/zfb_skm.png" alt="Alipay QR Code" /></td>
<td style="padding: 5px;background-color:#fff;"><img src="image/wx_zsm.png" alt="WeChat QR Code" /></td>
</tr>
</table>

---
##  📜 License

Copyright © 2025–2035 Luoyang Taishan TARZAN. All rights reserved.

This project is licensed under the GNU General Public License v3.0 (GPLv3). You may not use this project except in compliance with the License. A copy of the License is available at:

[🔗 https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)

Unless required by applicable law or agreed to in writing, software distributed under this License is provided "as is," without warranties or conditions of any kind, either express or implied. See the License for specific language governing permissions and limitations.

## 🔗 Related Resources
- 📘 [Open-source Model Library](https://modelscope.cn/models)
- 🐦 [MCP Plaza](https://modelscope.cn/mcp)
>  🌟 Star this repo to support China’s open-source AI ecosystem!</br>
> 🎯 Check this out! 👉 [Master Practical AI Large Model Application Development! 🔥](https://example.com/ai-guide)

---
✅ MaxKB4j — Empowering every Java team to effortlessly build enterprise-grade AI knowledge bases!