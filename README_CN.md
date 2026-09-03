# 🧠 MaxKB4j — 企业级智能问答系统：开箱即用的 RAG + LLM 工作流引擎

> **MaxKB4j = Max Knowledge Brain for Java**
> 一款基于 **Java 21 + Spring Boot 3（虚拟线程）** 构建的开箱即用、模型中立的 **RAG（检索增强生成）+ LLM 工作流引擎**，专为企业级智能问答系统而设计。
> 适用于智能客服、企业内部知识库、数据分析、学术研究与教育等场景。

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
  🚀 <a href="#快速开始">快速开始</a> · 🌐 <a href="http://43.143.235.194:8080/">在线 Demo</a>（demo / demo@123456） · 📄 <a href="docs/MaxKB4j-商业价值白皮书.md">商业价值白皮书</a> · 🧪 <a href="docs/MaxKB4j-回归测试报告.md">回归测试报告</a> · 🗒️ <a href="CHANGELOG.md">更新日志</a> · 💖 <a href="#支持与赞助">支持项目</a>
</p>

---

## ❓ 为什么选择 MaxKB4j？

企业落地大模型应用时，通常面临四类痛点，MaxKB4j 给出了对应答案：

| 痛点 | MaxKB4j 的答案 |
| :--- | :--- |
| **接入复杂**：主流方案依赖 Python / TS 生态，Java 团队上手成本高 | **纯 Java 原生**：Spring Boot 3 技术栈，现有 Java 工程师可直接二次开发，零跨语言成本 |
| **幻觉严重**：通用大模型不了解企业内部数据 | **生产级 RAG**：文档解析 → 分段 → 向量化 → 混合检索 → Reranker 精排，回答可溯源 |
| **并发瓶颈**：传统架构难以支撑高并发问答 | **虚拟线程 + 响应式架构**：单机可支撑数千级并发，资源占用低 |
| **功能单一**：只能单轮问答，无法编排复杂业务流程 | **可视化工作流 + 多 Agent 协作**：30+ 种节点类型覆盖复杂业务场景 |

## ✨ 核心能力

| 能力 | 说明 |
| :--- | :--- |
| 🔍 知识库问答 | 上传 PDF / Word / TXT / Markdown 等文档或自动爬取网页，自动完成分段 → 向量化 → 入库 → 构建 RAG，显著减少大模型幻觉 |
| 🧠 Advanced RAG / AgenticRAG | 向量、全文、混合多路召回 + Reranker 重排序；结合意图识别、条件分支，由智能体动态决策检索路径，支持多跳问答 |
| ⚙️ 可视化工作流 | 低代码编排 30+ 种节点：条件分支、循环、变量聚合、NL2SQL、表单、HTTP 请求、MCP 等，支持多轮与长期记忆 |
| 🤝 多 Agent 协作 | 多角色智能体（数据分析师、代码审查员、客服专员…）并行 / 串行协同，任务自动拆解、分派与汇总，共享记忆总线 |
| ⏰ 触发器 | Cron 定时任务 + Webhook 事件回调，实现智能体与工具无人值守自动化（自动生成日报、CRM 线索触发画像分析等） |
| 🌐 模型中立 | 私有模型（Ollama / Xorbits Inference / LocalAI）与国内外公有模型：通义千问、DeepSeek、豆包、混元、GLM、Kimi、GPT、Claude、Gemini 等 |
| 🧩 无缝集成 | RESTful API、iframe / Web SDK 嵌入组件、OpenAI 兼容对话接口、stream_http MCP 接入方式，5 分钟接入现有系统 |
| 🎙️ 多模态 | ASR 语音识别、TTS 语音合成、OCR 图像识别、Stable Diffusion 图像生成 |
| 🔒 权限与安全 | 基于 Sa-Token 的细粒度权限（应用 / 知识库 / 工具 / 模型）、审计日志、groovy-sandbox 脚本沙箱 |
| 🌱 生态扩展 | 数十种预置 Agent 模板（客服助手、数据分析师、代码导师等）；插件化工具市场：MySQL / PostgreSQL / MongoDB 连接器、飞书 / 钉钉 / 企业微信集成、联网搜索工具 |

## 📊 与主流方案的差异

| 能力 | **MaxKB4j** | Dify | MaxKB | FastGPT | RAGFlow |
| :--- | :---: | :---: | :---: | :---: | :---: |
| 后端技术栈 | **Java 21 + Spring Boot 3** | Python + TS | Python | TypeScript | Python |
| Java 团队零语言切换接入 | ✅ 原生 | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| 虚拟线程高并发架构 | ✅ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| 可视化工作流 + 多 Agent 协作 | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| 触发器（Cron / Webhook） | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| 多模态（ASR / TTS / OCR） | ✅ | ⚠️ | ✅ | ⚠️ | ✅ |
| MCP 协议 / OpenAI 兼容接口 | ✅ | ✅ | ✅ | ✅ | ✅ |

> 说明：上表为能力取向对比，供选型参考；具体效果建议结合自身场景通过在线 Demo 实测。

## 🚀 快速开始

### 环境要求
- Java 21+
- PostgreSQL 12+（启用 pgvector 扩展）
- MongoDB 6.0+（全文检索与文件存储）

### 方式一：Docker Compose（推荐）
```bash
docker-compose up -d
```
启动后访问 `http://localhost:8080/admin/login`（默认账号 `admin` / 密码 `tarzan@123456`），首次启动自动初始化数据库。

### 方式二：Docker 单容器
```bash
docker run --name maxkb4j -d --restart always -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/MaxKB4j \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  -e SPRING_DATA_MONGODB_URI=mongodb://admin:123456@localhost:27017/MaxKB4j?authSource=admin \
  registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j
```

### 方式三：一键安装脚本
`deploy/` 目录提供交互式安装脚本，自动完成环境检查、镜像拉取 / 构建与 docker-compose 编排（Linux / macOS / Windows）：

```bash
# Linux / macOS
chmod +x deploy/install.sh
./deploy/install.sh
# Windows
deploy\install.bat
```

### 方式四：源码构建
```bash
mvn clean package -DskipTests
java -jar maxkb4j-start/target/maxkb4j-start.jar
```
环境配置：`maxkb4j-start/src/main/resources/application-{dev,prod,test}.yml`，通过 `--spring.profiles.active=dev` 切换。

### 部署到第三方平台
支持 [一键部署到 Sealos](https://blog.csdn.net/weixin_40986713/article/details/156026021)（海外服务器、免网络代理、支持高并发与动态伸缩）。

## 🌐 在线 Demo 与 UI

- 在线 Demo：http://43.143.235.194:8080/（账号 `demo` / 密码 `demo@123456`，普通用户权限）
- 本地默认管理员：`admin` / `tarzan@123456`

<img src="image/maxkb4j.gif" alt="MaxKB4jUI" />

## 🛠 技术栈与项目结构

| 类别 | 技术 |
| :--- | :--- |
| 后端 | Java 21、Spring Boot 3、虚拟线程、Sa-Token |
| AI 框架 | LangChain4j 1.x、Docling 文档解析 |
| 存储 | PostgreSQL 15 + pgvector、MongoDB 6.0+、Caffeine 缓存 |
| 前端 | Vue 3、Node.js v20.16.0 |
| 脚本沙箱 | groovy-sandbox |

```
MaxKB4j/
├── maxkb4j-common / maxkb4j-core     # 通用工具、核心抽象与领域模型
├── maxkb4j-service/                  # 业务实现：application / chat / knowledge / model / oss / system / tool / trigger / workflow
├── maxkb4j-service-api/              # 对外契约：-api 模块（DTO / VO）
├── maxkb4j-start/                    # Spring Boot 启动入口、配置与打包
└── deploy/                           # 一键安装脚本（install.sh / install.bat）
```

> 依赖方向：`start` → `service` → `service-api` → `core` → `common`；对外契约放 `-api` 模块，实现放 `service` 模块。

## 📄 文档与资源

- 📄 [MaxKB4j 商业价值白皮书](docs/MaxKB4j-商业价值白皮书.md) —— 面向企业决策者的价值与选型分析
- 🧪 [MaxKB4j 回归测试报告](docs/MaxKB4j-回归测试报告.md) —— 核心功能回归验证结果
- 📐 [编码约定](docs/编码约定.md)
- 🗒️ [更新日志](CHANGELOG.md) —— 最新发布版本 v2.9.0（2026-06-17）

## 🤝 社区与贡献

| 类别 | 说明 |
| :--- | :--- |
| 反馈与需求 | 通过 [Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues) 提交 Bug、建议或新功能需求 |
| 贡献流程 | Fork → 创建分支 → 提交代码到 DEV 分支 → 发起 Pull Request |
| 开发规范 | 遵循 Alibaba Java 编码规范，包含单元测试并更新文档 |
| 交流群 | 添加作者微信 `vxhqqh` 加入核心交流群 |

## 💖 支持与赞助

> MaxKB4j 由个人开发者与社区成员共同维护，**没有商业公司背书**。
> 您的每一份支持都会直接投入：**云服务器成本、模型 Token 测试消耗、Bug 修复与新功能研发**，并会在交流群中定期公布使用情况。

| 档位 | 金额 | 核心权益 | 适合人群 |
| :---: | :---: | :--- | :--- |
| ☕ 咖啡支持 | ¥10 | 添加作者微信 `vxhqqh`、加入核心交流群、项目更新优先通知 | 认可项目价值的个人开发者 |
| 📚 学习会员 | ¥99 | 咖啡支持全部权益 + 免费加入 [知识星球](https://wx.zsxq.com/group/28882525858841) + 星球内问题优先解答 | 希望深度学习的开发者 |
| 🏢 企业合作伙伴 | ¥799 | 学习会员全部权益 + 一次性获取前端源码 + 部署 / 售后技术支持 | 企业用户 / 生产环境使用者 |
| 👑 战略合作 | ¥1399 | 企业合作伙伴全部权益 + 6 个月内前端源码免费升级 + 企业 Logo 展示于赞助墙 | 深度合作伙伴 |

**如何赞助**
1. 选择档位，扫描下方支付宝 / 微信赞赏码完成付款（建议备注昵称）
2. 添加作者微信 `vxhqqh`，发送付款截图，即刻开通对应权益

> 💡 付款后请务必联系作者，否则无法识别赞助身份、无法发放权益；企业用户如需发票、合同或对公支付，请联系作者确认。

<table>
  <tr>
    <th align="center">支付宝赞赏码</th>
    <th align="center">微信赞赏码</th>
  </tr>
  <tr>
    <td align="center"><img src="image/zfb_skm.png" alt="支付宝赞赏码" /></td>
    <td align="center"><img src="image/wx_zsm.png" alt="微信赞赏码" /></td>
  </tr>
</table>

**🏅 赞助墙**（按赞助时间排序，企业 Logo / 昵称将长期展示）
> 🎯 期待你的加入 —— 你的企业 Logo / 昵称将展示于此，获得社区持续曝光。

## 🏢 企业定制合作

需要**私有化部署、二次开发、团队培训、SLA 保障**等深度合作？可提供专属方案与报价：
- 私有化 / 内网环境部署
- 业务场景定制开发与系统集成
- 团队培训与知识转移
- 技术支持与 SLA 服务

联系作者微信 `vxhqqh`，获取专属方案与报价。

## 📜 许可证

Copyright © 2025–2035 洛阳泰山 TARZAN. All rights reserved.

本项目采用 [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) 许可证（“许可证”）。除非遵守许可证条款，否则不得使用本项目文件；依据本许可分发的软件按“原样”提供，不附带任何明示或暗示的担保或条件。

---

✅ **MaxKB4j — 轻松构建高性能且稳定的智能体工作流和 RAG 知识库解决方案**
