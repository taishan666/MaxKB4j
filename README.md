<p align="center"><img src= "https://github.com/1Panel-dev/maxkb/assets/52996290/c0694996-0eed-40d8-b369-322bf2a380bf" alt="MaxKB" width="300" /></p>
<h3 align="center">Ready-to-use, flexible RAG Chatbot</h3>
<h3 align="center">基于大模型和 RAG 的开源知识库问答系统</h3>
<p align="center"><a href="https://trendshift.io/repositories/9113" target="_blank"><img src="https://trendshift.io/api/badge/repositories/9113" alt="1Panel-dev%2FMaxKB | Trendshift" style="width: 250px; height: 55px;" width="250" height="55"/></a></p>
<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html#license-text"><img src="https://img.shields.io/github/license/1Panel-dev/maxkb?color=%231890FF" alt="License: GPL v3"></a>
  <a href="https://github.com/1Panel-dev/maxkb/releases/latest"><img src="https://img.shields.io/github/v/release/1Panel-dev/maxkb" alt="Latest release"></a>
  <a href="https://github.com/1Panel-dev/maxkb"><img src="https://img.shields.io/github/stars/1Panel-dev/maxkb?color=%231890FF&style=flat-square" alt="Stars"></a>    
  <a href="https://hub.docker.com/r/1panel/maxkb"><img src="https://img.shields.io/docker/pulls/1panel/maxkb?label=downloads" alt="Download"></a><br/>
 [<a href="/README_CN.md">中文(简体)</a>] | [<a href="/README.md">English</a>] 
</p>
<hr/>

MaxKB4J = Max Knowledge Base for Java, it is a chatbot based on Large Language Models (LLM) and Retrieval-Augmented Generation (RAG). MaxKB is widely applied in scenarios such as intelligent customer service, corporate internal knowledge bases, academic research, and education.

- **Ready-to-Use**: Supports direct uploading of documents / automatic crawling of online documents, with features for automatic text splitting, vectorization, and RAG (Retrieval-Augmented Generation). This effectively reduces hallucinations in large models, providing a superior smart Q&A interaction experience.
- **Flexible Orchestration**: Equipped with a powerful workflow engine and function library, enabling the orchestration of AI processes to meet the needs of complex business scenarios.
- **Seamless Integration**: Facilitates zero-coding rapid integration into third-party business systems, quickly equipping existing systems with intelligent Q&A capabilities to enhance user satisfaction.
- **Model-Agnostic**: Supports various large models, including private models (such as DeepSeek, Llama, Qwen, etc.) and public models (like OpenAI, Claude, Gemini, etc.).

## Quick start

Execute the script below to start a MaxKB4J container using Docker:

```bash
docker run -d --name=maxkb4j --restart=always -p 8080:8080 -v ~/.maxkb:/var/lib/postgresql/data -v ~/.python-packages:/opt/maxkb/app/sandbox/python-packages tarzan/maxkb
```

Access MaxKB web interface at `http://your_server_ip:8080` with default admin credentials:

- username: admin
- password: MaxKB4j@123..

中国用户如遇到 Docker 镜像 Pull 失败问题，请参照该 [离线安装文档](https://maxkb.cn/docs/installation/offline_installtion/) 进行安装。

## Problem consultation
![AI agent treasure trove](image/github_zsxq.png)

👉Claim the coupon：https://t.zsxq.com/YdmRl

## Screenshots


<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app.png" alt="MaxKB4j app"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/dataset.png" alt="MaxKB4j dataset"   /></td>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/function.png" alt="MaxKB4j function"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/dataset_setting.png" alt="MaxKB4j dataset_setting"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app_overview.png" alt="MaxKB4j app_overview"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app_logs.png" alt="MaxKB4j app_logs"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app_flow.png" alt="MaxKB4j app_flow"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app_simple.png" alt="MaxKB4j app_simple"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/doc.png" alt="MaxKB4j doc"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/doc_upload.png" alt="MaxKB4j doc_upload"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/hitTest.png" alt="MaxKB4j hitTest"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/problem.png" alt="MaxKB4j problem"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/team.png" alt="MaxKB4j team"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/model.png" alt="MaxKB4j model"   /></td>
  </tr>
 <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/user.png" alt="MaxKB4j user"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/email.png" alt="MaxKB4j email"   /></td>
  </tr>
</table>

## Technical stack

- Frontend：[Vue.js](https://vuejs.org/)
- Backend：[Java17 / Springboot3](https://www.djangoproject.com/)
- LLM Framework：[LangChain4j](https://docs.langchain4j.dev/)
- Database：[PostgreSQL + pgvector](https://www.postgresql.org/)
- Full-text retrieval database：[MongoDB](https://www.mongodb.com/)
- User authentication：[sa-token](https://sa-token.dev33.cn/)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=1Panel-dev/MaxKB&type=Date)](https://star-history.com/#1Panel-dev/MaxKB&Date)

## License

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

<https://www.gnu.org/licenses/gpl-3.0.html>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
