# 🧠 MaxKB4j — Enterprise-Grade Intelligent Q&A System: Out-of-the-Box RAG + LLM Workflow Engine

> **MaxKB4j = Max Knowledge Brain for Java**
> A ready-to-use, secure, model-agnostic **RAG (Retrieval-Augmented Generation) + LLM workflow engine**, purpose-built for enterprise-grade intelligent Q&A systems.
> Widely used in scenarios such as intelligent customer service, internal enterprise knowledge bases, data analysis, academic research, and education.



<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html#license-text"><img src="https://img.shields.io/badge/License-GPL%20v3-blue" alt="License: GPL v3"></a>
  <a href=""><img src="https://img.shields.io/badge/Java-17+-green" alt="Java 17+"></a>
  <a href=""><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x"></a>
  <a href=""><img src="https://img.shields.io/badge/Langchain4J-1.x-green" alt="LangChain4j"></a><br/>
 [<a href="/README_CN.md">中文(简体)</a>] | [<a href="/README.md">English</a>]
</p>

---
## 💡 Why Choose MaxKB4j?
In today's AI application boom, are you facing these challenges?
- ❌ Complex Integration: Existing solutions rely on Python ecosystem, making it costly for Java teams to get started?
- ❌ Serious Hallucinations: Generic large models answer inaccurately and cannot integrate with internal enterprise data?
- ❌ Concurrency Bottlenecks: Traditional architectures struggle to support high-concurrency scenarios with high response latency?
- ❌ Limited Functionality: Only simple Q&A, unable to handle complex business workflows and multi-Agent collaboration?

**MaxKB4j Provides You with a One-Stop Solution:**

Built on **Java 21 + Spring Boot 3 + Virtual Threads**, perfectly integrating **RAG (Retrieval-Augmented Generation)** with **visual workflow**. Empower your applications with AI capabilities of "understanding, reasoning, and execution" without modifying existing systems.

---

## ✨ Core Features
| Feature Category | Detailed Description                                                                                                                                                                                                                                                                                       |
| :--- |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ⏰ Triggers | • **Scheduled Task Trigger**: Supports configuring Cron expressions or visual timeline for unattended automation of agents and tools (e.g., daily automatic data report generation, scheduled competitor information crawling).<br>• **Event Callback Trigger**: Supports Webhook integration with external system events for real-time response (e.g., automatically trigger customer profiling Agent when new leads are added in CRM, trigger alert notifications when database data changes).                                                                                                                              |
| 🔍 Out-of-the-Box Knowledge Base Q&A | • Supports uploading local documents (PDF/Word/TXT/Markdown, etc.)<br>• Supports automatic web content crawling<br>• Supports custom workflow knowledge base writing<br>• Automatically handles: text chunking → vectorization → storage in vector database → RAG pipeline construction<br>• Significantly reduces LLM "hallucinations", improves answer accuracy and reliability                                                                                                                                             |
| ⚡ High Concurrency & High Performance | • Built on Java 21 + Spring Boot 3 + Virtual Threads (Project Loom), fully leveraging modern JVM's lightweight concurrency capabilities for significantly improved throughput and response speed.<br>• Adopts reactive programming model (Reactor) and asynchronous non-blocking I/O, effectively handling thousands of concurrent requests with lower resource usage and lower latency.<br>• Built-in multi-level caching mechanism to accelerate knowledge retrieval and model call chains.                                                                                                          |
| 🌐 Model-Agnostic & Flexible Integration | Supports various mainstream large language models, including:<br>• **Local Private Models**: DeepSeek-R1, Llama 3, Qwen 2, etc. (via Ollama / Xorbits Inference / LocalAI)<br>• **Chinese Public Models**: Tongyi Qianwen, Tencent HunYuan, ByteDance Doubao, Baidu Qianfan, Zhipu GLM, Kimi, DeepSeek, etc.<br>• **International Public Models**: OpenAI (GPT), Anthropic (Claude), Google (Gemini)                                                                          |
| ⚙️ Visual Workflow Orchestration | • Built-in low-code AI workflow engine, supports conditional branching, function calling, multi-turn conversation memory<br>• Provides rich built-in function library (HTTP requests, database queries, time processing, regex extraction, etc.)<br>• Suitable for complex business scenarios: customer support ticket generation, data report interpretation, internal policy Q&A, etc.                                                                                                                                                                      |
| 🤝 Multi-Agent Collaboration | • Built-in Multi-Agent collaboration framework, supports multiple specialized AI Agents working in parallel or sequentially<br>• Each Agent can be configured with independent roles (e.g., data analyst, code reviewer, customer service specialist), dedicated knowledge bases and toolsets<br>• Supports dynamic task distribution and context-aware Agent routing, complex tasks are automatically decomposed, assigned, and aggregated (e.g., user question → requirement understanding Agent → data query Agent → report generation Agent)<br>• Provides inter-Agent communication mechanism and shared memory bus, ensuring information consistency and collaboration coherence<br>• Suitable for advanced scenarios: cross-department process automation, end-to-end product design, joint fault diagnosis, etc. |
| 🧩 Seamless Integration into Existing Systems | • Provides RESTful API and frontend embedding components (iframe / Web SDK)<br>• No need to modify existing systems, integrate intelligent Q&A capabilities in 5 minutes<br>• Provides OpenAI-compatible dialogue interface                                                                                                                                                                                               |
| 🤖 Skill Tools | • Supports [MCP](https://modelcontextprotocol.io/) protocol, enabling AI to understand code context, project structure, and dependencies<br>• Supports local code function programming tool calls<br>• Supports HTTP interface tool calls<br>• Supports Claude SKILLS skill calls (in beta testing)                                                                                                                                                |
| 🎙️ Multimodal Extensions | • Speech Recognition (ASR), Speech Synthesis (TTS)<br>• Image Recognition (OCR), Image Generation (Stable Diffusion)                                                                                                                                                                                                                                |
| 🔒 User Permission Management | • Fine-grained permission control (application / knowledge base / tool / model)<br>• Audit logs, authentication and authorization (based on Sa-Token)                                                                                                                                                                                                                                  |
| 🌱 Ecosystem Extensions (Extensibility & Out-of-the-Box) | • Rich Agent template library: Provides dozens of pre-built Agent templates (e.g., customer service assistant, data analyst, code mentor, meeting note taker), one-click enable, quick adaptation to business scenarios.<br>• Flexible plugin tool marketplace: Supports dynamic loading of functional modules through plugin mechanism, including:<br>✅ Data connectors (MySQL, PostgreSQL, MongoDB, etc.)<br>✅ Third-party service integrations (Feishu, DingTalk, WeCom)<br>✅ Web search tools (Google Search, SearchApi, SearXNg, etc.)                                                                   |


---

## 🚀 Quick Start

### 1. System Requirements
- Java 21+
- PostgreSQL 12+ (with pgvector extension enabled)
- MongoDB 6.0+ (optional, for full-text search)

### 2. Deployment

#### 2.1 Local Startup (JAR Mode)
```bash
# Start the application
java -jar maxkb4j-start.jar
```

#### 2.2 Docker Deployment
```bash
docker run --name maxkb4j -d --restart always -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=123456  -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j
```
- The first 8080 in `-p 8080:8080` is the host port, the second 8080 is the container port
- `-e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=123456` are PostgreSQL database connection parameters, can be modified as needed
- `-e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin` is MongoDB connection parameter, can be modified as needed

#### 2.3 Docker-Compose Deployment (Recommended)
```yaml
# See docker-compose.yml example in project root directory
docker-compose up -d
```

#### 2.4 Deploy to Third-Party Platforms
<details>
<summary><strong>Deploy to Sealos</strong></summary>
<div>

> Sealos servers are located overseas, no need to handle network issues separately, supports high concurrency & dynamic scaling.

Click the button below for one-click deployment:

[![Deploy on Sealos](https://img-home.csdnimg.cn/images/20230724024159.png?origin_url=https%3A%2F%2Fhzh.sealos.run%2Flogo.svg&pos_id=img-ulndzmCF-1768647863738) ](https://blog.csdn.net/weixin_40986713/article/details/156026021)

</div>
</details>

### 3. Access Web Interface
- URL: http://localhost:8080/admin/login
- Default username: `admin`
- Default password: `tarzan@123456`

> On first launch, the database (PostgreSQL + MongoDB) will be automatically initialized, please ensure ports are not occupied.


---

## 🛠 Tech Stack

| Category | Technology                                   |
|------|--------------------------------------|
| **Backend** | Java 21, Spring Boot 3, Sa-Token (Authentication) |
| **AI Framework** | LangChain4j                          |
| **Vector Database** | PostgreSQL 15 + pgvector             |
| **Full-Text Search** | MongoDB 5.0+                         |
| **Caching** | Caffeine                             |
| **Frontend** | Vue 3, Node.js v20.16.0              |


---

## 📸 UI Preview
<img src= "image/maxkb4j.gif" alt="MaxKB4j team"   />

---
## 🤝 Contributing Guide

We welcome community contributions! If you have suggestions, bug reports, or new feature requests, please submit them via [Issue](https://gitee.com/taisan/MaxKB4j/issues) or directly submit a Pull Request.

| Category | Description |
| :--- | :--- |
| 🎯 How to Contribute | Fix bugs, develop new features, improve documentation, write tests, or optimize UI/UX. |
| 📋 Process | Fork project → Create branch → Commit changes → Push branch → Open Pull Request. |
| 🎨 Standards | Follow Alibaba Java Coding Guidelines, include unit tests, and update documentation. |

## 💖 Support & Sponsorship

> **🌟 Open source is not easy, persistence is harder**
> MaxKB4j is maintained by individual developers and community members. Your support will be directly used for server costs, token testing consumption, API testing, bug fixes, and new feature development!


| Tier |  Amount   | Core Benefits                                                                                        | Target Audience |
|:---:|:-----:|:--------------------------------------------------------------------------------------------|:---|
| ☕ Coffee Support |  ¥10  | • Add author on WeChat `vxhqqh`<br>• Join core discussion group (mention "sponsored")<br>• Priority notification of project updates                                       | Individual developers who recognize project value |
| 📚 Learning Member |  ¥50  | • All Coffee Support benefits<br>• Free access to [Knowledge Planet🔥](https://wx.zsxq.com/group/28882525858841)<br>• Priority answers to questions in the Planet | Developers who want to learn in depth |
| 🔧 Advanced Developer | ¥200  | • All Learning Member benefits<br>• Project deployment assistance <br>• Priority technical support response <br>• Participate in new feature requirement discussions                                     | Developers who want to deeply understand architecture |
| 🏢 Enterprise Partner | ¥600  | • All Advanced Developer benefits<br>• V2 complete frontend and backend source code <br>• Issue after-sales technical support                                 | Enterprise users / Advanced users |
| 👑 Strategic Partner | ¥1200 | • All Enterprise Partner benefits <br>• Free project upgrades within one year <br>• Enterprise logo displayed on official website sponsor wall                                           | Deep cooperation partners |


<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <th style="padding: 10px;"> <div align="center">Alipay QR Code</div></th>
    <th style="padding: 10px;"> <div align="center">WeChat QR Code</div></th>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src="image/zfb_skm.png" alt="Alipay QR Code"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/wx_zsm.png" alt="WeChat QR Code"   /></td>
  </tr>
</table>


> Sponsorship amount is only used for continuous project development and maintenance, not for profit purposes. 💡 Open source is not easy, thank you for every support!

---

## 📜 License

Copyright © 2025–2035 Luoyang Taishan TARZAN. All rights reserved.

Licensed under the GNU General Public License Version 3 (GPLv3) ("License"); you may not use this project file except in compliance with the License. You may obtain a copy of the License at

[🔗https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)

Unless required by applicable law or agreed to in writing, software distributed under the License is provided on an "AS IS" basis, without warranties or conditions of any kind, either express or implied. See the License for the specific language governing permissions and limitations under the License.

---

## 🔗 Related Resources

- 📘 [Open-source Model Library](https://modelscope.cn/models)
- 🐦 [MCP Plaza](https://modelscope.cn/mcp)
- 🌐 [Skills](https://skills.sh/)

>  🌟 **Star this project to support China's open-source AI ecosystem!** </br>
> 🎯 **Check this out! 👉 [Learn about AI large model application development in practice! 🔥](https://example.com/ai-guide)**

---

✅ **MaxKB4j — Easily build high-performance and stable agent workflows and RAG knowledge base solutions**