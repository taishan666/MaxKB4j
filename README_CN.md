
# 🧠 MaxKB4j — 基于 Java 的开源 RAG 知识库与 LLM 工作流平台

> **MaxKB4j = Max Knowledge Base for Java**  
> 一个开箱即用、安全可靠、模型中立的 **RAG（检索增强生成）+ LLM 工作流引擎**，专为构建企业级智能问答系统而设计。
> 广泛应用于 智能客服、企业内部知识库、数据分析、学术研究与教育等场景。



<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html#license-text"><img src="https://img.shields.io/badge/License-GPL%20v3-blue" alt="License: GPL v3"></a>
  <a href=""><img src="https://img.shields.io/badge/Java-17+-green" alt="Java 17+"></a>
  <a href=""><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x"></a>    
  <a href=""><img src="https://img.shields.io/badge/Langchain4J-1.x-green" alt="Nodejs"></a><br/>
 [<a href="/README_CN.md">中文(简体)</a>] | [<a href="/README.md">English</a>] 
</p>

---

## ✨ 核心特性

### 🔍 开箱即用的知识库问答
- 支持 **上传本地文档**（PDF/Word/TXT/Markdown 等）或 **自动爬取网页内容**
- 自动完成：文本分段 → 向量化 → 存入向量数据库 → 构建 RAG 流程
- 显著减少大模型“幻觉”，提升回答准确性与可信度

### 🌐 模型中立，灵活对接
支持各类主流大模型，包括：
- **本地私有模型**：DeepSeek-R1、Llama 3、Qwen 2 等（通过 Ollama / LM Studio / vLLM）
- **国内公有模型**：通义千问、腾讯混元、字节豆包、百度千帆、智谱 GLM、Kimi
- **国际公有模型**：OpenAI (GPT)、Anthropic (Claude)、Google (Gemini)

> 只需配置 API Key 或本地端点，即可无缝切换模型！

### ⚙️ 可视化工作流编排
- 内置 **低代码 AI 工作流引擎**，支持条件分支、函数调用、多轮对话记忆
- 提供丰富 **内置函数库**（HTTP 请求、数据库查询、时间处理、正则提取等）
- 适用于复杂业务场景：客服工单生成、数据报告解读、内部制度问答等

### 🧩 无缝嵌入现有系统
- 提供 **RESTful API** 和 **前端嵌入组件（iframe / Web SDK）**
- 无需改造原有系统，5 分钟集成智能问答能力

### 🤖 MCP Server 支持（Model Context Protocol）
- 支持 [MCP](https://modelcontextprotocol.io/) 协议，让 AI 理解**代码上下文**、项目结构、依赖关系
- 不再只是“聊天机器人”，而是真正的 **AI 编程协作者**

### 🎙️ 多模态扩展（规划中）
- 已支持：语音识别（ASR）、语音合成（TTS）、图像识别（OCR）、图像生成（Stable Diffusion）
- 视频生成模型支持正在开发中…

---

## 🚀 快速开始

### 1. 环境要求
- Java 17+
- Maven 或 Gradle
- PostgreSQL 12+（启用 pgvector 扩展）
- MongoDB 6.0+（可选，用于全文检索）

### 2. 启动服务
```bash
# 启动应用
java -jar MaxKB4j.jar
```

### 3. 基于Docker 部署
```bash
docker run --name maxkb4j -d --restart always -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=123456  -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:2.0
```
其中，`-p 8080:8080` 中的第一个 8080 是宿主机的端口，`-e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=123456` 是PostgreSQL数据库的连接配置参数，`-e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin`是MongoDB的连接配置参数， 可以根据需要进行修改。

### 4. Docker-Compose 部署（推荐）
```yaml
# docker-compose.yml 示例见项目根目录
docker-compose up -d
```

### 5. 访问 Web 界面
- 地址：http://localhost:8080/admin/login
- 默认账号：`admin`
- 默认密码：`tarzan@123456`

> 首次启动会自动初始化数据库（PostgreSQL + MongoDB），请确保端口未被占用。

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| **后端** | Java 17, Spring Boot 3, Sa-Token（鉴权） |
| **AI 框架** | LangChain4j |
| **向量数据库** | PostgreSQL 15 + pgvector |
| **全文检索** | MongoDB |
| **缓存** | Caffeine |
| **前端** | Vue 3, Node.js v20.16.0 |

---

## 📸 UI 展示
<img src= "image/maxkb4j.gif" alt="MaxKB4j team"   />

> 更多界面请参考项目 Wiki 或实际部署体验。

---

## ❓ 问题与建议

欢迎提交 Issue 或 PR！  
👉 [Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues)

---

## 💖 支持与赞助

本项目由个人开发者维护，您的支持将帮助项目持续迭代！

| 赞助金额 | 权益 |
|--------|------|
| ¥20 | 添加作者微信（`vxhqqh`），加入交流群（备注“已赞助”） |
| ¥50 | 上述权益 + 免费加入[《👉 知识星球🔥》](https://wx.zsxq.com/group/28882525858841)   |
| ¥200 | 获取 **V1 版本前端源码** |
| ¥500 | 获取 **V2 完整前后端源码**（含最新功能） |


<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <th style="padding: 10px;"> <div align="center">支付宝赞赏码</div></th>
    <th style="padding: 10px;"> <div align="center">微信赞赏码</div></th>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src="image/zfb_skm.png" alt="支付宝赞赏码"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/wx_zsm.png" alt="微信赞赏码"   /></td>
  </tr>
</table>


---

## 📜 许可证

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

根据GNU通用公共许可证版本3 (GPLv3)（“许可证”）进行许可；除非遵守许可，否则您不得使用此项目文件。您可以从以下网址获得许可证的副本

[🔗https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)

除非适用法律要求或经书面同意，依据本许可分发的软件均按“原样”提供，不附带任何形式的明示或暗示的担保或条件。有关许可下具体权限和限制的条款，请参见本许可协议。

---

## 🔗 相关资源

- 📘 [官方文档（规划中）]()
- 🐦 [作者微信公众号]()
- 🌟 **Star 本项目，助力国产开源 AI 生态！**

> 🎯 **看看这个！👉 [点击了解 AI 大模型应用开发实战！🔥](https://example.com/ai-guide)**

---

✅ **MaxKB4j — 让每个 Java 团队都能轻松构建企业级 AI 知识库！**