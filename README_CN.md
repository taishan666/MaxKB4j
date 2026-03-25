
# 🧠 MaxKB4j — 企业级智能问答系统：开箱即用的 RAG + LLM 工作流引擎

> **MaxKB4j = Max Knowledge Brain for Java**  
> 一个开箱即用、安全可靠、模型中立的 **RAG（检索增强生成）+ LLM 工作流引擎**，专为构建企业级智能问答系统而设计。
> 广泛应用于 智能客服、企业内部知识库、数据分析、学术研究与教育等场景。



<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html#license-text"><img src="https://img.shields.io/badge/License-GPL%20v3-blue" alt="License: GPL v3"></a>
  <a href=""><img src="https://img.shields.io/badge/Java-17+-green" alt="Java 17+"></a>
  <a href=""><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x"></a>    
  <a href=""><img src="https://img.shields.io/badge/Langchain4J-1.x-green" alt="LangChain4j"></a><br/>
 [<a href="/README_CN.md">中文(简体)</a>] | [<a href="/README.md">English</a>] 
</p>

---
## 💡 为什么选择 MaxKB4j？
在 AI 应用爆发的今天，您是否面临以下挑战？
- ❌ 接入复杂：现有方案依赖 Python 生态，Java 团队上手成本高？
- ❌ 幻觉严重：通用大模型回答不准确，无法结合企业内部数据？
- ❌ 并发瓶颈：传统架构难以支撑高并发场景，响应延迟高？
- ❌ 功能单一：仅能简单问答，无法处理复杂业务流程和多 Agent 协作？

**MaxKB4j 为您提供一站式解决方案：**

基于 **Java 21 + Spring Boot 3 + 虚拟线程** 构建，完美融合 **RAG（检索增强生成）** 与 **可视化工作流**。无需改造原有系统，即可赋予您的应用“理解、推理、执行”的 AI 能力。

---

## ✨ 核心特性
| 特性类别 | 详细描述                                                                                                                                                                                                                                                                                       |
| :--- |:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ⏰ 触发器功能 (Triggers) | • 定时任务触发：支持配置 Cron 表达式或可视化时间轴，实现智能体与工具的无人值守自动化（如：每日自动生成数据报告、定时爬取竞品信息）。<br>• 事件回调触发：支持 Webhook 接入外部系统事件，实现实时响应（如：当 CRM 新增线索时自动触发客户画像分析 Agent、当数据库数据变更时触发预警通知）。                                                                                                                              |
| 🔍 开箱即用的知识库问答 | • 支持上传本地文档（PDF/Word/TXT/Markdown 等）<br>• 支持自动爬取网页内容<br>• 支持自定义工作流知识库写入<br>• 自动完成：文本分段 → 向量化 → 存入向量数据库 → 构建 RAG 流程<br>• 显著减少大模型“幻觉”，提升回答准确性与可信度                                                                                                                                             |
| ⚡ 高并发高性能 | • 基于 Java 21 + Spring Boot 3 + 虚拟线程（Project Loom）构建，充分利用现代 JVM 的轻量级并发能力，显著提升吞吐量与响应速度。<br>• 采用 响应式编程模型（Reactor） 与 异步非阻塞 I/O，有效应对数千级并发请求，资源占用更低、延迟更小。<br>• 内置 多级缓存机制，加速知识检索与模型调用链路。                                                                                                          |
| 🌐 模型中立，灵活对接 | 支持各类主流大模型，包括：<br>• 本地私有模型：DeepSeek-R1、Llama 3、Qwen 2 等（通过 Ollama / Xorbits Inference / LocalAI）<br>• 国内公有模型：通义千问、腾讯混元、字节豆包、百度千帆、智谱 GLM、Kimi、DeepSeek等<br>• 国际公有模型：OpenAI (GPT)、Anthropic (Claude)、Google (Gemini)                                                                          |
| ⚙️ 可视化工作流编排 | • 内置 低代码 AI 工作流引擎，支持条件分支、函数调用、多轮对话记忆<br>• 提供丰富 内置函数工具库（HTTP 请求、数据库查询、时间处理、正则提取等）<br>• 适用于复杂业务场景：客服工单生成、数据报告解读、内部制度问答等                                                                                                                                                                      |
| 🤝 多 Agent 协作能力 | • 内置 多智能体（Multi-Agent）协作框架，支持多个专业化 AI Agent 并行或串行协同工作<br>• 每个 Agent 可配置独立角色（如：数据分析师、代码审查员、客服专员）、专属知识库与工具集<br>• 支持 动态任务分发 与 上下文感知的 Agent 路由，复杂任务自动拆解、分派、汇总（例如：用户提问 → 需求理解 Agent → 数据查询 Agent → 报告生成 Agent）<br>• 提供 Agent 间通信机制 与 共享记忆总线，确保信息一致性与协作连贯性<br>• 适用于高阶场景：跨部门流程自动化、端到端产品设计、联合故障诊断等 |
| 🧩 无缝嵌入现有系统 | • 提供 RESTful API 和 前端嵌入组件（iframe / Web SDK）<br>• 无需改造原有系统，5 分钟集成智能问答能力<br>• 提供兼容Open AI的对话接口                                                                                                                                                                                               |
| 🤖 技能工具 | • 支持 [MCP](https://modelcontextprotocol.io/) 协议，让 AI 理解代码上下文、项目结构、依赖关系<br>• 支持本地代码函数编程工具调用<br>• 支持HTTP接口工具调用<br>• 支持Claude SKILLS技能调用                                                                                                                                                      |
| 🎙️ 多模态扩展 | • 语音识别（ASR）、语音合成（TTS）<br>• 图像识别（OCR）、图像生成（Stable Diffusion）                                                                                                                                                                                                                                |
| 🔒 用户权限管理 | • 细粒度权限控制（应用 / 知识库 / 工具 / 模型）<br>• 审计日志、认证授权（基于 Sa-Token）                                                                                                                                                                                                                                  |
| 🌱 生态扩展（可扩展性与开箱即用） | • 丰富的智能体模板库：提供数十种预置 Agent 模板（如客服助手、数据分析师、代码导师、会议纪要员等），一键启用，快速适配业务场景。<br>• 灵活的插件化工具市场：支持通过插件机制动态加载功能模块，涵盖：<br>✅ 数据连接器（MySQL、PostgreSQL、MongoDB 等）<br>✅ 第三方服务集成（飞书、钉钉、企业微信）<br>✅ 网络搜素工具（Google Search、SearchApi、SearXNg 等）                                                                   |


---

## 🚀 快速开始

### 1. 环境要求
- Java 21+
- PostgreSQL 12+（启用 pgvector 扩展）
- MongoDB 6.0+（可选，用于全文检索）

### 2. 部署启动

#### 2.1 本地启动（JAR 方式）
```bash
# 启动应用
java -jar maxkb4j-start.jar
```

#### 2.2. 基于Docker 部署
```bash
docker run --name maxkb4j -d --restart always -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=123456  -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j
```
- `-p 8080:8080` 中的第一个 8080 是宿主机的端口，第二个 8080 是docker容器开发的端口
- `-e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=123456` 是PostgreSQL数据库的连接配置参数， 可以根据需要进行修改
- `-e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin`是MongoDB的连接配置参数， 可以根据需要进行修改

#### 2.3. Docker-Compose 部署（推荐）
```yaml
# docker-compose.yml 示例见项目根目录
docker-compose up -d
```

#### 2.4. 部署到第三方平台
<details>
<summary><strong>部署到 Sealos </strong></summary>
<div>

> Sealos 的服务器在国外，不需要额外处理网络问题，支持高并发 & 动态伸缩。

点击以下按钮一键部署：

[![](https://sealos.run/app_store/img/sealos.svg)](https://blog.csdn.net/weixin_40986713/article/details/156026021)
</div>
</details>

### 3. 访问 Web 界面
- 地址：http://localhost:8080/admin/login
- 默认账号：`admin`
- 默认密码：`tarzan@123456`

> 首次启动会自动初始化数据库（PostgreSQL + MongoDB），请确保端口未被占用。


---

## 🛠 技术栈

| 类别 | 技术                                   |
|------|--------------------------------------|
| **后端** | Java 21, Spring Boot 3, Sa-Token（鉴权） |
| **AI 框架** | LangChain4j                          |
| **向量数据库** | PostgreSQL 15 + pgvector             |
| **全文检索** | MongoDB 5.0+                         |
| **缓存** | Caffeine                             |
| **前端** | Vue 3, Node.js v20.16.0              |


---

## 📸 UI 展示
<img src= "image/maxkb4j.gif" alt="MaxKB4j team"   />

---
## 🤝 贡献指南

我们欢迎社区用户参与贡献！如有建议、Bug 或新功能需求，请通过 [Issue](https://gitee.com/taisan/MaxKB4j/issues) 提出，或直接提交 Pull Request。

| 类别 | 说明 |
| :--- | :--- |
| 🎯 如何贡献 | 修复 Bug、开发新功能、完善文档、编写测试或优化 UI/UX。 |
| 📋 流程 | Fork 项目 → 创建分支 → 提交代码 → 推送分支 → 发起 Pull Request。 |
| 🎨 规范 | 遵循 Alibaba Java 编码规范，包含单元测试并更新文档。 |

## 💖 支持与赞助

> **🌟 开源不易，坚持更难**  
> MaxKB4j 由个人开发者与社区成员**维护**，您的支持将直接用于服务器成本、Token测试消耗、API 测试、Bug 修复与新功能研发等！


| 档位 |  金额   | 核心权益                                                                                        | 适合人群 |
|:---:|:-----:|:--------------------------------------------------------------------------------------------|:---|
| ☕ 咖啡支持 |  ¥10  | • 添加作者微信 `vxhqqh`<br>• 加入核心交流群（备注"已赞助"）<br>• 项目更新优先通知                                       | 认可项目价值的个人开发者 |
| 📚 学习会员 |  ¥50  | • 咖啡支持全部权益<br>• 免费加入 [《👉 知识星球🔥》](https://wx.zsxq.com/group/28882525858841)<br>• 星球内问题优先解答 | 希望深度学习的开发者 |
| 🔧 进阶开发者 | ¥200  | • 学习会员全部权益<br>• 项目部署上线协助 <br>• 技术答疑优先响应 <br>• 参与新功能需求讨论                                     | 想深入理解架构的开发者 |
| 🏢 企业伙伴 | ¥650  | • 进阶开发者全部权益<br>• V2 完整前后端源码 <br>• 问题售后技术支持                                 | 企业用户/深度使用者 |
| 👑 战略合作 | ¥1299 | • 企业伙伴全部权益 <br>• 一年内项目免费升级 <br>• 企业 Logo 展示于官网赞助墙                                           | 深度合作伙伴 |


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


> 赞助金额仅用于项目持续开发和维护，非盈利用途。💡 开源不易，感谢每一份支持！

---

## 📜 许可证

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

根据GNU通用公共许可证版本3 (GPLv3)（“许可证”）进行许可；除非遵守许可，否则您不得使用此项目文件。您可以从以下网址获得许可证的副本

[🔗https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)

除非适用法律要求或经书面同意，依据本许可分发的软件均按“原样”提供，不附带任何形式的明示或暗示的担保或条件。有关许可下具体权限和限制的条款，请参见本许可协议。

---

## 🔗 相关资源

- 📘 [开源模型库](https://modelscope.cn/models)
- 🐦 [MCP 广场](https://modelscope.cn/mcp)
- 🌐 [Skills 中心](https://modelscope.cn/skills)

>  🌟 **Star 本项目，助力国产开源 AI 生态！** </br>
> 🎯 **看看这个！👉 [点击了解 AI 大模型应用开发实战！🔥](https://example.com/ai-guide)**

---

✅ **MaxKB4j — 轻松构建高性能且稳定的智能体工作流和RAG知识库解决方案**
