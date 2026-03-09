SQLBot AI 智能体
一、应用描述
本应用专为 SQLBot 系统用户设计，依托 AI 自然语言交互能力，简化查询信息的流程。用户无需手动操作系统表单，仅需用自然语言描述要查询的信息，AI 智能体便会调用 MCP 工具在 SQLBot 系统中进行查询，降低系统操作门槛，适配销售、客服等岗位的业务需求。

二、应用功能
SQLBot AI 智能体应用具有以下功能：

查询信息：用户通过自然语言描述要查询的信息，AI 智能体会调用 SQLBot MCP 工具，写出 SQL 查询语句，将查询结果返回给用户，并根据数据画出用户指定的图形，最后进行分析总结。
三、应用构建要素
SQLBot AI 智能体应用构建时涉及的核心要素内容：

大模型：deepseek-chat
MCP ：SQLBot MCP: https://dataease.cn/sqlbot/v1/mcp_server/
需要涉及到的参数：

参数名	类型	说明
username	str	SQLBot的 用户名
password	str	SQLBot的 密码

工作流：
![img.png](/admin/app/sql_bot_assistant/img.png)
四、应用效果

![sqlbot_ai.gif](/admin/app/sql_bot_assistant/sqlbot_ai.gif)