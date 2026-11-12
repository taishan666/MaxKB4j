-- 用户 Token 排行：按应用和会话创建时间筛选。
CREATE INDEX "application_chat_app_create_time_idx"
    ON "public"."application_chat" USING btree ("application_id" ASC, "create_time" ASC);

-- 覆盖排行 join 与 Token 聚合所需列，避免读取包含大文本字段的聊天记录宽行。
CREATE INDEX "application_chat_record_chat_tokens_idx"
    ON "public"."application_chat_record" USING btree ("chat_id" ASC)
    INCLUDE ("message_tokens", "answer_tokens");
