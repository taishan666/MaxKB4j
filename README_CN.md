<h3 align="center">基于大模型和 RAG 的知识库问答系统</h3>
<h4 align="center">开箱即用、灵活的RAG知识库加工作流聊天机器人</h4>


MaxKB4j = Max Knowledge Base for Java，是一款基于Java语言开发的LLM工作流应用和 RAG 的开源LLMOps平台，项目主要借鉴了MaxKB和FastGPT,并将两个的优势结合到一个项目上，使用高性能、高稳定性以及安全可靠的JAVA语言重新设计开发。MaxKB4j广泛应用于 **智能客服、企业内部知识库、数据分析、学术研究与教育等场景** 。

- **开箱即用**：支持直接上传文档 / 自动爬取在线文档，支持文本自动拆分、向量化和 RAG（检索增强生成），有效减少大模型幻觉，智能问答交互体验好；
- **模型中立**：支持对接各种大模型，包括本地私有大模型（DeekSeek R1 / Llama 3 / Qwen 2 等）、国内公共大模型（通义千问 / 腾讯混元 / 字节豆包 / 百度千帆 / 智谱 AI / Kimi 等）和国外公共大模型（OpenAI / Claude / Gemini 等）；
- **灵活编排**：内置强大的工作流引擎和函数库，支持编排 AI 工作过程，满足复杂业务场景下的需求；
- **无缝嵌入**：支持零编码快速嵌入到第三方业务系统，让已有系统快速拥有智能问答能力，提高用户满意度。
- **支持接入MCP Server**：MCP（Model Context Protocol，模型上下文协议）是一个用于 AI 与开发环境交互的标准协议，让 AI 具备代码上下文的感知能力，而不只是单纯地做代码补全或聊天问答。
- **多种模型支持**：支持语音识别和语音合成模型、支持图像识别和图像生成模型。（视频生成模型支持规划中。。。）


目前已经完成所有核心的功能的开发，正在完善优化项目，预计月底完成，之后会完善文档和示例，欢迎大家参与完善。
- dev 分支目前基于maxkb v2分支代码改造，工作量比较大，目前请使用v1分支的代码


## 功能导图
![输入图片说明](image/MaxKB4J.png)

## 快速开始

```
java -jar maxkb4j-1.0.0.jar
```
- 访问地址 - 访问地址 http://localhost:8080/login
- 默认用户/密码 admin/maxkb4j.


## 案例展示

MaxKB4j 自发布以来，日均安装下载超过 1000 次，被广泛应用于智能客服、企业内部知识库、学术教育研究等场景。

todo

## UI 展示


<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/app.png" alt="MaxKB4j app"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/dataset.png" alt="MaxKB4j dataset"   /></td>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/function.png" alt="MaxKB4j function"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/mcp.png" alt="MaxKB4j dataset_setting"   /></td>
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

## 技术栈

- 前端：[Vue.js](https://cn.vuejs.org/)
- 后端：[Java17 / Springboot3](https://www.djangoproject.com/)
- 缓存：[caffeine](https://github.com/ben-manes/caffeine)
- LangChain4j：[LangChain4j](https://docs.langchain4j.dev/)
- 向量数据库：[PostgreSQL / pgvector](https://www.postgresql.org/)
- 全文检索数据库：[MongoDB](https://www.mongodb.com/)
- 用户鉴权：[sa-token](https://sa-token.dev33.cn/)

## 我的技术专栏

- [《看看这个！👉 点击AI大模型应用开发！🔥》](https://blog.csdn.net/weixin_40986713/category_12606825.html)

- **《AI语音合成与识别》**》：[https://blog.csdn.net/weixin_40986713/category_12735457.html](https://blog.csdn.net/weixin_40986713/category_12735457.html)

- **《AI绘画 | Stable diffusion》**：[https://blog.csdn.net/weixin_40986713/category_12481790.html](https://blog.csdn.net/weixin_40986713/category_12481790.html)

## 问题和建议

请提交 issues [https://gitee.com/taisan/MaxKB4j/issues](https://gitee.com/taisan/MaxKB4j/issues)

## 咨询和反馈
- **这个项目我已独自坚持了半年多，期间尝试找合伙人未果——毕竟没人能长期免费投入。<br>我之前的开源项目，也因缺乏反馈和收益而被迫放弃。<br>如果你觉得这个项目有价值，请给个 Star 支持，或加入知识星球深度参与。<br>
你的支持，不只是鼓励，更是它能否持续发展的关键。<br>若长期无人支持，可能只能无奈停更。<br>一个 Star，一次加入，都能让它走得更远。**
- **提供MaxKB相关问题的咨询解答，协助部署等。**

<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <th style="padding: 10px;"> <div align="center">知识星球</div></th>
    <th style="padding: 10px;"> <div align="center">微信赞赏码</div></th>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src="image/gitee_zsxq.png" alt="知识星球"   /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "image/wx_zsm.jpg" alt="微信赞赏码"   /></td>
  </tr>
</table>

## 🙏 致谢

感谢以下各位对本项目资金捐赠等各种方式的支持！以下排名不分先后：
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/36436022" target="_blank"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAYAAAA4qEECAAADiklEQVR4AezaW0gUURgH8G93cfEWYrIlQWh0I1AQQYSQLCILLCgEA6unLhBFBBEk5ENWQhEpQQhpT3Z5MXpQjKwgKcsLmaSQlRhBFCJBeV2tdduz4MiOIQ7L+XvC/8Jh95s5c74zv/mz7oPujC17gxz6DdzCF0SA0BBmEUITGiQAasNEExokAGrDRBMaJABqw0QTGiQAarOoiQbdoxFtCA16DIQmNEgA1IaJJjRIANSGiSY0SADUhokmNEgA1IaJJrRmAfDyTDQInNCEBgmA2jDRhAYJgNow0YRemEDN9QvS0/LQGs8e1EpudubCLgbOYqJB2IQmNEgA1IaJXhRoUNOl2IaJBj11QhMaJABqw0QTGiQAasNEExokAGrDRBMaJABqw0QvPWjQHS9SGyYaBE9oQoMEQG2YaEKDBEBtjEl02upVUn21TBru3JTXTXeltbFO9hRsBTHob2MMdKovRTasTZP0EHhiQrx4vV7xpSSL09fvPwEZG59wepn2+cZA2+/UGxMjqSt99sNz6qRliRHH/P5J6e3rjzhmQmEMdHtXj/waHrVM3G6XbFq/xqr/9SEvN1tW+JZHnBoc+hFRm1IYA61Avn4fVG/W2LguXQ4UFVq1/cPuHfmSnJRkHQ4EAvJp4ItVm/TBKOhXHd0yPuG3fOJiY+Xk4RIpPXVEEuLjrOM5WRlSW1kuu7bniUr+zImfwyPS9ubdTOnkXftco6Abmp/Lh/7PETet/jCWhFKtfoW0PbonHY/vy+2q8vD/13ncs9sPTE9La8dbedneFXG9KcXsTg3Ykfq1cKuuXr4NDs3ZjcfjCadapdzlckWcDwaD0hn6jq+oqok4blJhFLSCUYk8X3FD3n8cEAWojs03JqempOnpCzlddsXIn3UzezcOWm2ss7tXio+ekfJr1dLd2ycjo2OivhrUOTX8k1Ph1Dc2t8jxsxfl3KVKo5HVno2EVhtTo77xiRw6USqbCw9K1rYiyczfFx45BftlZ/ExKb1cJZ2hh6Lmmj6MhjYdz8n+CO1EK4q5hI4Cz8mlhHaiFcVcQkeB5+RSQjvRimLufNBRLMtL7QKEtotoqgmtCda+LKHtIppqQmuCtS9LaLuIpprQmmDtyxLaLqKpJrQmWPuyhLaLaKoJrQnWvqyx0PaN/u81oUFPkNCEBgmA2jDRhAYJgNow0YQGCYDaMNGEBgmA2jDRIOi/AAAA//9aKOcFAAAABklEQVQDAEimQM65kprcAAAAAElFTkSuQmCC" width="80px;" alt="金鸿伟"/><br /><sub><b>金鸿伟</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="javascript:void(0)" target="_blank"><img src=".image/sponsor/Best%20Yao.jpg" width="80px;" alt="Best Yao"/><br /><sub><b>Best Yao</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/weiloser" target="_blank"><img src="data:image/png;base64,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" width="80px;" alt="无为而治"/><br /><sub><b>无为而治</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/shup092_admin" target="_blank"><img src="https://foruda.gitee.com/avatar/1715249792671224067/1875078_shup092_admin_1715249792.png!avatar200" width="80px;" alt="shup"/><br /><sub><b>shup</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/gampa" target="_blank"><img src="https://foruda.gitee.com/avatar/1756193534973241962/13981826_gampa_1756193534.png!avatar200" width="80px;" alt="也许"/><br /><sub><b>也许</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/leishaozhuanshudi" target="_blank"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAYAAAA4qEECAAAB10lEQVR4AezaMUoDURRG4TsDRmIQS2OrjY26B+s0LkWwsoiYwtaluANba61sbDW9haUShQGbIQTvyZN3hCeBhDmZb/4y7et0/9OTb9CGf4iA0AhzhNBCQwJQxkULDQlAGRctNCQAZVy00JAAlFnroqF7LCIjNPQYhBYaEoAyLlpoSADKuGihIQEo46KFhgSgjIsWOlkAvryLhsCFFhoSgDIuWmhIAMq46Bqht0/PYzx9jr3rl+8zvnyK4ckZRJGbcdG5vt3Vhe4ocl8InevbXV3ojiL3xW/o3FbVVxcaevxCCw0JQBkXLTQkAGVctNCQAJRx0UJDAlDGRdcHDd3xmjIuGoIXWmhIAMq4aKEhASjjooWGBKCMixYaEoAyLlpoSOAnk/6/6EU3g63Ymcxi8Ru8Vc7uxUMMjybpiMsEioaOpokFdrM5ipXOYBTRbizjkP6ZsqHTb58LCA1ZFwX9fn8b89lhvF0d/MmZ3xzHx+MdRNmfKQq6/6v+73eFhp6f0EJDAlDGRQsNCUCZvkVDX6GOjNDQcxZaaEgAyrhooSEBKOOihYYEoIyLFhoSgDIuunZo6P6xjIuGqIUWGhKAMi5aaEgAyrhooSEBKOOihYYEoIyLhqC/AAAA//82AnliAAAABklEQVQDAFLvJYaZ8M0oAAAAAElFTkSuQmCC" width="80px;" alt="⁰ʚᦔrꫀꪖꪑ⁰ɞ ."/><br /><sub><b>⁰ʚᦔrꫀꪖꪑ⁰ɞ .</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/fateson" target="_blank"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAYAAAA4qEECAAAB/UlEQVR4Aezbvy4EURxH8V/2IYhOstFJ9HgD9TYUnkAjGnoq0etoNCLRKLyA0CkoV6PansSfXdyYKBQrG7P37E3mbHamwR7zmW81oTV1tPvpkd+gFb4QAaER5gihhYYEoIyLFhoSgDIuWmhIAMq4aKEhASgz0UVD11hERmjoNggtNCQAZVy00JAAlHHRQkMCUMZFCw0JQBkXLXRmAfjjXTQELrTQkACUcdFCQwJQxkU3DbrTno/u6lb01nfGetx2NmJ5ZjYm/XLR0B0QWuiIl0E/nt7fah3P6ef7HwOIc3im6EWfPdxH+2S/1rF0fhjXvcfhAtBXfkND0SZmhIbuutBCQwJQxkULDQlAGRctNCQAZVy00BFrcwv/fpJ3sLgCEY6WcdGjOdX+roKga19L0R9QNHSdp3ev6clfSfJFQ9d5erd9c1mSs/90T92NohdNIRAdoQnl1BA6IRBvoQnl1BA6IRBvoQnl1BA6IRBvoSvl7GehsxNXAaErh+xnobMTVwGhK4fs52KgT7t3339jN328Fz/H5tVFdgAqUAw0dcGT6ggNyQstNCQAZVy00JAAlPlr0dCv0IyM0NB9FlpoSADKuGihIQEo46KFhgSgjIsWGhKAMi666dDQ9WMZFw1RCy00JABlXLTQkACUcdFCQwJQxkULDQlAGRcNQX8BAAD//8JCKMwAAAAGSURBVAMADg2tKHB55VAAAAAASUVORK5CYII=" width="80px;" alt="逆"/><br /><sub><b>逆</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/dongGezzz_admin" target="_blank"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAYAAAA4qEECAAAEkUlEQVR4AezaW2wMURgH8H+ne6tqVSu9UXGLqpciKRGEukbEkwe3xCUkEiJBIp5488ILCV7c2gQPEg88EEWIS5vggaB1a117U9VWVbfb7a5zNiY7U7qJlf3O9PSb9HTOnN3ON+c3X8+enRnj5NqrYS6JNzDAC4kAQ5MwAwzN0EQCRGE4oxmaSIAoDGc0QxMJEIXhjGZoIgGiMEozmqiPjgjD0ESngaEZmkiAKAxnNEMTCRCF4YxmaCIBojCc0QxNJEAUhjOaoRMsQLx7zmgicIZmaCIBojCc0QxNJEAUhjN6qECvOjwPWy8s/+eyuXwZNp5egtVH5mPx7hmYOCefiCy+MIM2o5PdBtwpLqRlD8O4khyUbi/G+hMLUbxyApy4GE48qLiOKQlIyfCiZE0hVuyfhYzRw+PaTaL+yEjUjuPdb19vCL3dwZgl2NMHhAeIIMDzijKxcOc0R2E7Drr5VRvKt9yIWco2V+DU+mu4evAham5+RHd7zx/wmQVpmL2xCG6fa4AzQttsh6aN/d/RGl604sGZF7i45y5qKxsQCoai+xSZnT81C7M3FEXbFNYGNbTp1usP4vbxp6itakQ4FB1TkowkFMzIjnxYmu9VtdYC2sSrPFuNhupW2zCSkubB+Fl55luUrbWClpldc+MjujsDUVAxhORMzkD2pIxom4KaVtDS7/2jZjSKsVvWzeJL9yBXzETMbRVr7aAlYpOYuUSmgHJDFJcnGaPGpYuauh8toTsauxAQc3Era2qWz7pJXtcSuv7ZV/R09dowU9K9GJGXClWLltASs7tDfImRld8l2WMgNVNdVmsLDct3F2ltuAy4vMlQtegL3U/UJa72eVLd/VrpNh0ETddpFZGGDLSchfz85ldhHImpLbTKYSIi2++XltByGucRd1+sfZUZLa/2Wdso61pCZ45Ng3e4/YOv/3SPElnG0hba7bNM5cSV0+9NP2V/lRUtofOKsmCIebOpGhDXq7+8bjM3lay1g5aPHYwssN+Y7Wr1493DZiXAZlCtoOX9wcIFY+C1fDEJh8NoevkN8lq12WkVa62gS9YWIrdwpM3xR4sfb+7W29pUbGgBLTO5dEcxppSOsY3N8mZtXVUDvrxtV2FrizmoocfPzMWC7cVYfXR+5JEw6wegfO6jUQwZTy7X2To8wEbCmx0HnSP+9eUzdbHKprNLsfX8cizaNR2T5ubDJ27A2qTEdE7eZakqr1E+NpvH5Tho85k6+VzdQMUlL3eKm65mJ6zrUF8Yn5624PaxJ2iv/2F9SWndcdDxasjnOdo+deLB6ee4fugxuhReQPpbHwYtdDDQB39nAC21Haiu+IArB6pwad99vLrz+W/9VN6mHPrS3ns4te7aP5eyTRU4t+0WLu+vRGVZNVrqOpRjxjoA5dCxDk6n1xia6GwyNEMTCRCF4YxmaCIBojCxMproEIZGGIYmOs8MzdBEAkRhOKMZmkiAKAxnNEMTCRCF4YxmaCIBojCc0UMdmqj/ZGE4o4moGZqhiQSIwnBGMzSRAFEYzmiGJhIgCsMZzdBEAkRhOKOJoH8BAAD//0t1bq8AAAAGSURBVAMAGjsyPRTgzoQAAAAASUVORK5CYII=" width="80px;" alt="廖东旺"/><br /><sub><b>廖东旺</b></sub></a></td>
      <td align="center" valign="top" width="11.11%"><a href="https://gitee.com/huangzhen1993" target="_blank"><img src="https://foruda.gitee.com/avatar/1755927793103396536/5094937_huangzhen1993_1755927793.png!avatar200" width="80px;" alt="黄振"/><br /><sub><b>黄振</b></sub></a></td>
    </tr>
  </tbody>
</table>

- 赞赏10元以上，可以联系我上捐赠榜单
## License

Copyright (c) 2025-2035 洛阳泰山 TARZAN, All rights reserved.

Licensed under The GNU General Public License version 3 (GPLv3)  (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at

<http://www.apache.org/licenses>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
