
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
## 🔥 为什么选择 MaxKB4j？

| 传统方案痛点 | MaxKB4j 解决方案                                               |
|--------------|------------------------------------------------------------|
| 需要大量代码集成 RAG | ✅ 开箱即用，上传文档即可问答                                            |
| 模型绑定，迁移成本高 | ✅ 模型中立，自由切换本地/公有大模型                                        |
| 无法处理复杂业务逻辑 | ✅ 可视化工作流引擎，支持条件分支、工具调用等                                    |
| 难以嵌入现有系统 | ✅ 提供 REST API + Web SDK，5 分钟集成                             |
| AI 无法理解项目上下文 | ✅ 支持 [MCP 协议](https://modelcontextprotocol.io/)，成为真正的编程协作者 |

---

## ✨ 核心特性

### 🔍 开箱即用的知识库问答
- 支持 **上传本地文档**（PDF/Word/TXT/Markdown 等）
- 支持 **自动爬取网页内容**
- 支持 **自定义工作流知识库写入**
- 自动完成：文本分段 → 向量化 → 存入向量数据库 → 构建 RAG 流程
- 显著减少大模型“幻觉”，提升回答准确性与可信度

### ⚡ 高并发高性能

- 基于 **Java 17 + Spring Boot 3 + 虚拟线程（Project Loom）** 构建，充分利用现代 JVM 的轻量级并发能力，显著提升吞吐量与响应速度。
- 采用 **响应式编程模型（Reactor）** 与 **异步非阻塞 I/O**，有效应对数千级并发请求，资源占用更低、延迟更小。
- 内置 **多级缓存机制**，加速知识检索与模型调用链路。


### 🌐 模型中立，灵活对接
支持各类主流大模型，包括：
- **本地私有模型**：DeepSeek-R1、Llama 3、Qwen 2 等（通过 Ollama / Xorbits Inference / LocalAI）
- **国内公有模型**：通义千问、腾讯混元、字节豆包、百度千帆、智谱 GLM、Kimi、DeepSeek等
- **国际公有模型**：OpenAI (GPT)、Anthropic (Claude)、Google (Gemini)

### ⚙️ 可视化工作流编排
- 内置 **低代码 AI 工作流引擎**，支持条件分支、函数调用、多轮对话记忆
- 提供丰富 **内置函数工具库**（HTTP 请求、数据库查询、时间处理、正则提取等）
- 适用于复杂业务场景：客服工单生成、数据报告解读、内部制度问答等

### 🤝 多 Agent 协作能力
- 内置 **多智能体（Multi-Agent）协作框架**，支持多个专业化 AI Agent 并行或串行协同工作
- 每个 Agent 可配置独立角色（如：数据分析师、代码审查员、客服专员）、专属知识库与工具集
- 支持 **动态任务分发** 与 **上下文感知的 Agent 路由**，复杂任务自动拆解、分派、汇总（例如：用户提问 → 需求理解 Agent → 数据查询 Agent → 报告生成 Agent）
- 提供 **Agent 间通信机制** 与 **共享记忆总线**，确保信息一致性与协作连贯性
- 适用于高阶场景：跨部门流程自动化、端到端产品设计、联合故障诊断等

### 🧩 无缝嵌入现有系统
- 提供 **RESTful API** 和 **前端嵌入组件（iframe / Web SDK）**
- 无需改造原有系统，5 分钟集成智能问答能力

### 🤖 技能工具
- 支持 [MCP](https://modelcontextprotocol.io/) 协议，让 AI 理解**代码上下文**、项目结构、依赖关系
- 支持本地代码函数编程工具调用

### 🎙️ 多模态扩展
-  语音识别（ASR）、语音合成（TTS）
-  图像识别（OCR）、图像生成（Stable Diffusion）

### 🔒 用户权限管理
- 细粒度权限控制（应用 / 知识库 / 工具 / 模型）
- 审计日志、认证授权（基于 Sa-Token）

---

## 🚀 快速开始

### 1. 环境要求
- Java 17+
- PostgreSQL 12+（启用 pgvector 扩展）
- MongoDB 6.0+（可选，用于全文检索）

### 2. 部署启动

#### 2.1 本地启动（JAR 方式）
```bash
# 启动应用
java -jar MaxKB4j.jar
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

[![外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传](https://img-home.csdnimg.cn/images/20230724024159.png?origin_url=https%3A%2F%2Fhzh.sealos.run%2Flogo.svg&pos_id=img-ulndzmCF-1768647863738) ](https://blog.csdn.net/weixin_40986713/article/details/156026021)

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
| **后端** | Java 17, Spring Boot 3, Sa-Token（鉴权） |
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

### 🎯 贡献方式

- 🐛 **Bug修复**: 发现并修复系统缺陷
- ✨ **新功能**: 提出并实现新特性
- 📚 **文档改进**: 完善项目文档
- 🧪 **测试用例**: 编写单元测试和集成测试
- 🎨 **UI/UX优化**: 改进用户界面和体验

### 📋 贡献流程

1. **Fork项目** 到你的GitHub账户
2. **创建特性分支** `git checkout -b feature/amazing-feature`
3. **提交更改** `git commit -m 'Add amazing feature'`
4. **推送分支** `git push origin feature/amazing-feature`
5. **创建Pull Request** 并详细描述变更内容

### 🎨 代码规范

- 遵循 **alibaba java coding guidelines**规则 
- 添加必要的单元测试
- 更新相关文档


## 💖 支持与赞助

> 本项目由个人开发者维护，您的支持将帮助项目持续迭代！


| 赞助金额 | 权益                                                                 |
|------|--------------------------------------------------------------------|
| ¥10  | 添加作者微信（`vxhqqh`），加入交流群（备注“已赞助”）                                    |
| ¥50  | 上述权益 + 免费加入[《👉 知识星球🔥》](https://wx.zsxq.com/group/28882525858841) |
| ¥600 | 上述权益 +  **V2 完整前后端源码**  + **售后支持**                                      |


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
- 🐦 [MCP广场](https://modelscope.cn/mcp)

>  🌟 **Star 本项目，助力国产开源 AI 生态！** </br>
> 🎯 **看看这个！👉 [点击了解 AI 大模型应用开发实战！🔥](https://example.com/ai-guide)**

---

✅ **MaxKB4j — 轻松构建高性能且稳定的智能体工作流和RAG知识库解决方案**