
-- ----------------------------
-- 创建vector 扩展
-- ----------------------------
CREATE EXTENSION IF NOT EXISTS "vector";
-- ----------------------------
-- Table structure for system_setting
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_setting";
CREATE TABLE "public"."system_setting" (
                                           "type" int4 NOT NULL,
                                           "meta" jsonb NOT NULL,
                                           "create_time" timestamp(6) NOT NULL,
                                           "update_time" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table system_setting
-- ----------------------------
ALTER TABLE "public"."system_setting" ADD CONSTRAINT "system_setting_pkey" PRIMARY KEY ("type");


-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS "public"."user";
CREATE TABLE "public"."user" (
                                 "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "email" varchar(254) COLLATE "pg_catalog"."default",
                                 "phone" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                 "nickname" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "username" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "password" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "role" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "is_active" bool NOT NULL,
                                 "source" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
                                 "language" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
                                 "create_time" timestamp(6),
                                 "update_time" timestamp(6)
)
;

-- ----------------------------
-- Indexes structure for table user
-- ----------------------------
CREATE INDEX "user_email_like" ON "public"."user" USING btree (
    "email" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "user_username_like" ON "public"."user" USING btree (
    "username" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table user
-- ----------------------------
ALTER TABLE "public"."user" ADD CONSTRAINT "user_email_key" UNIQUE ("email");
ALTER TABLE "public"."user" ADD CONSTRAINT "user_username_key" UNIQUE ("username");

-- ----------------------------
-- Primary Key structure for table user
-- ----------------------------
ALTER TABLE "public"."user" ADD CONSTRAINT "user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for user_resource_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_resource_permission";
CREATE TABLE "public"."user_resource_permission" (
                                                     "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "workspace_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "auth_target_type" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "target_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "auth_type" varchar COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'ROLE'::character varying,
                                                     "permission_list" varchar(256)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                     "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "create_time" timestamp(6) NOT NULL,
                                                     "update_time" timestamp(6) NOT NULL
)
;



-- ----------------------------
-- Primary Key structure for table user_resource_permission
-- ----------------------------
ALTER TABLE "public"."user_resource_permission" ADD CONSTRAINT "workspace_user_resource_permission_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for application
-- ----------------------------
DROP TABLE IF EXISTS "public"."application";
CREATE TABLE "public"."application" (
                                        "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                        "desc" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
                                        "prologue" varchar(4096) COLLATE "pg_catalog"."default" NOT NULL,
                                        "dialogue_number" int4 NOT NULL,
                                        "knowledge_setting" jsonb NOT NULL,
                                        "model_setting" jsonb NOT NULL,
                                        "problem_optimization" bool NOT NULL DEFAULT false,
                                        "model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "icon" varchar(256) COLLATE "pg_catalog"."default",
                                        "type" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                        "work_flow" jsonb NOT NULL,
                                        "model_params_setting" jsonb NOT NULL,
                                        "stt_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "stt_model_enable" bool NOT NULL DEFAULT false,
                                        "tts_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "tts_model_enable" bool NOT NULL DEFAULT false,
                                        "tts_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                        "problem_optimization_prompt" varchar(102400) COLLATE "pg_catalog"."default",
                                        "tts_model_params_setting" jsonb NOT NULL,
                                        "clean_time" int4 NOT NULL,
                                        "file_upload_enable" bool NOT NULL DEFAULT false,
                                        "file_upload_setting" jsonb NOT NULL,
                                        "tts_autoplay" bool NOT NULL DEFAULT false,
                                        "stt_auto_send" bool NOT NULL DEFAULT false,
                                        "tool_ids" varchar[] NOT NULL,
                                        "tool_output_enable" bool NOT NULL DEFAULT true,
                                        "folder_id" varchar(64) COLLATE "pg_catalog"."default",
                                        "is_publish" bool NOT NULL DEFAULT false,
                                        "publish_time" timestamp(6),
                                        "create_time" timestamp(6) NOT NULL,
                                        "update_time" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application
-- ----------------------------
CREATE INDEX "application_model_id" ON "public"."application" USING btree (
    "model_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_stt_model_id" ON "public"."application" USING btree (
    "stt_model_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_tts_model_id" ON "public"."application" USING btree (
    "tts_model_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_user_id" ON "public"."application" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application
-- ----------------------------
ALTER TABLE "public"."application" ADD CONSTRAINT "application_pkey" PRIMARY KEY ("id");



-- ----------------------------
-- Table structure for application_access_token
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_access_token";
CREATE TABLE "public"."application_access_token" (
                                                     "create_time" timestamp(6) NOT NULL,
                                                     "update_time" timestamp(6) NOT NULL,
                                                     "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "access_token" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "is_active" bool NOT NULL,
                                                     "access_num" int4 NOT NULL,
                                                     "white_active" bool NOT NULL DEFAULT false,
                                                     "white_list" varchar(128)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                     "show_source" bool NOT NULL DEFAULT true,
                                                     "language" varchar(10) COLLATE "pg_catalog"."default",
                                                     "show_exec" bool NOT NULL DEFAULT true,
                                                     "authentication" bool NOT NULL  DEFAULT false
)
;

-- ----------------------------
-- Indexes structure for table application_access_token
-- ----------------------------
CREATE INDEX "application_access_token_access_token_like" ON "public"."application_access_token" USING btree (
    "access_token" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table application_access_token
-- ----------------------------
ALTER TABLE "public"."application_access_token" ADD CONSTRAINT "application_access_token_access_token_key" UNIQUE ("access_token");

-- ----------------------------
-- Primary Key structure for table application_access_token
-- ----------------------------
ALTER TABLE "public"."application_access_token" ADD CONSTRAINT "application_access_token_pkey" PRIMARY KEY ("application_id");

-- ----------------------------
-- Foreign Keys structure for table application_access_token
-- ----------------------------
ALTER TABLE "public"."application_access_token" ADD CONSTRAINT "application_access_token_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for application_api_key
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_api_key";
CREATE TABLE "public"."application_api_key" (
                                                "create_time" timestamp(6) NOT NULL,
                                                "update_time" timestamp(6) NOT NULL,
                                                "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "secret_key" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                "is_active" bool NOT NULL,
                                                "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "allow_cross_domain" bool NOT NULL DEFAULT false,
                                                "cross_domain_list" varchar(128)[] COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_api_key
-- ----------------------------
CREATE INDEX "application_api_key_application_id" ON "public"."application_api_key" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_api_key_secret_key_like" ON "public"."application_api_key" USING btree (
    "secret_key" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_api_key_user_id" ON "public"."application_api_key" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table application_api_key
-- ----------------------------
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_secret_key_key" UNIQUE ("secret_key");

-- ----------------------------
-- Primary Key structure for table application_api_key
-- ----------------------------
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_api_key
-- ----------------------------
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_chat
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat";
CREATE TABLE "public"."application_chat" (
                                             "create_time" timestamp(6) NOT NULL,
                                             "update_time" timestamp(6) NOT NULL,
                                             "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "summary" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
                                             "chat_user_id" varchar COLLATE "pg_catalog"."default",
                                             "chat_user_type" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                             "is_deleted" bool NOT NULL DEFAULT false,
                                             "asker" jsonb NOT NULL,
                                             "meta" jsonb NOT NULL,
                                             "star_num" int4 NOT NULL,
                                             "trample_num" int4 NOT NULL,
                                             "chat_record_count" int4 NOT NULL,
                                             "mark_sum" int4 NOT NULL,
                                             "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_chat
-- ----------------------------
CREATE INDEX "application_chat_application_id" ON "public"."application_chat" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "application_chat_create_time" ON "public"."application_chat" USING btree (
    "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "application_chat_update_time" ON "public"."application_chat" USING btree (
    "update_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_chat
-- ----------------------------
ALTER TABLE "public"."application_chat" ADD CONSTRAINT "application_chat_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for application_chat_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat_record";
CREATE TABLE "public"."application_chat_record" (
                                                    "create_time" timestamp(6) NOT NULL,
                                                    "update_time" timestamp(6) NOT NULL,
                                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "vote_status" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "problem_text" varchar(10240) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "answer_text" varchar(40960) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "message_tokens" int4 NOT NULL,
                                                    "answer_tokens" int4 NOT NULL,
                                                    "cost" int4 NOT NULL,
                                                    "details" jsonb NOT NULL,
                                                    "improve_paragraph_id_list" varchar(50)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                    "run_time" float8 NOT NULL,
                                                    "index" int4 NOT NULL,
                                                    "chat_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "answer_text_list" varchar[] COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Indexes structure for table application_chat_record
-- ----------------------------
CREATE INDEX "application_chat_record_chat_id" ON "public"."application_chat_record" USING btree (
    "chat_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_chat_record
-- ----------------------------
ALTER TABLE "public"."application_chat_record" ADD CONSTRAINT "application_chat_record_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for application_chat_user_stats
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat_user_stats";
CREATE TABLE "public"."application_chat_user_stats" (
                                                        "create_time" timestamp(6) NOT NULL,
                                                        "update_time" timestamp(6) NOT NULL,
                                                        "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "access_num" int4 NOT NULL,
                                                        "intra_day_access_num" int4 NOT NULL,
                                                        "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "chat_user_id" varchar(50) COLLATE "pg_catalog"."default",
                                                        "chat_user_type" varchar(64) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Indexes structure for table application_chat_user_stats
-- ----------------------------
CREATE INDEX "application_public_access_client_application_id" ON "public"."application_chat_user_stats" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_chat_user_stats
-- ----------------------------
ALTER TABLE "public"."application_chat_user_stats" ADD CONSTRAINT "application_public_access_client_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_chat_user_stats
-- ----------------------------
ALTER TABLE "public"."application_chat_user_stats" ADD CONSTRAINT "application_chat_user_stats_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for application_version
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_version";
CREATE TABLE "public"."application_version" (
                                                "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                "publish_user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "publish_user_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                "workspace_id" varchar(64) COLLATE "pg_catalog"."default",
                                                "application_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                "desc" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
                                                "prologue" varchar(40960) COLLATE "pg_catalog"."default" NOT NULL,
                                                "dialogue_number" int4 NOT NULL,
                                                "model_id" varchar(50) COLLATE "pg_catalog"."default",
                                                "knowledge_setting" jsonb NOT NULL,
                                                "model_setting" jsonb NOT NULL,
                                                "model_params_setting" jsonb NOT NULL,
                                                "tts_model_params_setting" jsonb NOT NULL,
                                                "problem_optimization" bool NOT NULL DEFAULT false,
                                                "icon" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                                "work_flow" jsonb NOT NULL,
                                                "type" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                                "problem_optimization_prompt" varchar(102400) COLLATE "pg_catalog"."default",
                                                "tts_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                                "stt_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                                "tts_model_enable" bool NOT NULL DEFAULT false,
                                                "stt_model_enable" bool NOT NULL DEFAULT false,
                                                "tts_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                                "tts_autoplay" bool NOT NULL DEFAULT false,
                                                "stt_auto_send" bool NOT NULL DEFAULT false,
                                                "clean_time" int4 NOT NULL,
                                                "file_upload_enable" bool NOT NULL DEFAULT false,
                                                "file_upload_setting" jsonb NOT NULL,
                                                "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "user_id" varchar(50) COLLATE "pg_catalog"."default",
                                                "tool_ids" varchar[] NOT NULL,
                                                "tool_output_enable" bool NOT NULL DEFAULT true,
                                                "create_time" timestamp(6) NOT NULL,
                                                "update_time" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table application_version
-- ----------------------------
ALTER TABLE "public"."application_version" ADD CONSTRAINT "application_version_pkey" PRIMARY KEY ("id");



-- ----------------------------
-- Table structure for knowledge
-- ----------------------------
DROP TABLE IF EXISTS "public"."knowledge";
CREATE TABLE "public"."knowledge" (
                                      "create_time" timestamp(6) NOT NULL,
                                      "update_time" timestamp(6) NOT NULL,
                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                      "desc" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                      "meta" jsonb NOT NULL,
                                      "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "embedding_model_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "file_size_limit" int4 DEFAULT 100,
                                      "file_count_limit" int4 DEFAULT 50,
                                      "type" int2,
                                      "folder_id" varchar(64) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Indexes structure for table knowledge
-- ----------------------------
CREATE INDEX "knowledge_embedding_mode_id" ON "public"."knowledge" USING btree (
    "embedding_model_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "knowledge_user_id" ON "public"."knowledge" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table knowledge
-- ----------------------------
ALTER TABLE "public"."knowledge" ADD CONSTRAINT "knowledge_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table knowledge
-- ----------------------------
ALTER TABLE "public"."knowledge" ADD CONSTRAINT "knowledge_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS "public"."document";
CREATE TABLE "public"."document" (
                                     "create_time" timestamp(6) NOT NULL,
                                     "update_time" timestamp(6) NOT NULL,
                                     "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                     "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                     "char_length" int4 NOT NULL,
                                     "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                     "is_active" bool NOT NULL,
                                     "type" int2 NOT NULL,
                                     "meta" jsonb NOT NULL,
                                     "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                     "hit_handling_method" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                     "directly_return_similarity" float8 NOT NULL,
                                     "status_meta" jsonb NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table document
-- ----------------------------
CREATE INDEX "document_knowledge_id" ON "public"."document" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table document
-- ----------------------------
ALTER TABLE "public"."document" ADD CONSTRAINT "document_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table document
-- ----------------------------
ALTER TABLE "public"."document" ADD CONSTRAINT "document_knowledge_id_fk_knowledge_id" FOREIGN KEY ("knowledge_id") REFERENCES "public"."knowledge" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;




-- ----------------------------
-- Table structure for paragraph
-- ----------------------------
DROP TABLE IF EXISTS "public"."paragraph";
CREATE TABLE "public"."paragraph" (
                                      "create_time" timestamp(6) NOT NULL,
                                      "update_time" timestamp(6) NOT NULL,
                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "content" varchar(102400) COLLATE "pg_catalog"."default" NOT NULL,
                                      "title" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                      "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                      "hit_num" int4 NOT NULL,
                                      "is_active" bool NOT NULL,
                                      "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "status_meta" jsonb,
                                      "position" int4 DEFAULT 1
)
;

-- ----------------------------
-- Indexes structure for table paragraph
-- ----------------------------
CREATE INDEX "paragraph_knowledge_id" ON "public"."paragraph" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "paragraph_document_id" ON "public"."paragraph" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table paragraph
-- ----------------------------
ALTER TABLE "public"."paragraph" ADD CONSTRAINT "paragraph_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table paragraph
-- ----------------------------
ALTER TABLE "public"."paragraph" ADD CONSTRAINT "paragraph_knowledge_id_fk_knowledge_id" FOREIGN KEY ("knowledge_id") REFERENCES "public"."knowledge" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;




-- ----------------------------
-- Table structure for problem
-- ----------------------------
DROP TABLE IF EXISTS "public"."problem";
CREATE TABLE "public"."problem" (
                                    "create_time" timestamp(6) NOT NULL,
                                    "update_time" timestamp(6) NOT NULL,
                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "content" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                    "hit_num" int4 NOT NULL,
                                    "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table problem
-- ----------------------------
CREATE INDEX "problem_knowledge_id" ON "public"."problem" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table problem
-- ----------------------------
ALTER TABLE "public"."problem" ADD CONSTRAINT "problem_pkey" PRIMARY KEY ("id");



-- ----------------------------
-- Table structure for folder
-- ----------------------------
DROP TABLE IF EXISTS "public"."folder";
CREATE TABLE "public"."folder" (
                                   "create_time" timestamp(6) NOT NULL,
                                   "update_time" timestamp(6) NOT NULL,
                                   "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                   "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                   "desc" varchar(200) COLLATE "pg_catalog"."default",
                                   "parent_id" varchar(64) COLLATE "pg_catalog"."default",
                                   "user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                   "source" varchar(64) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table folder
-- ----------------------------
CREATE INDEX "application_folder_create_time" ON "public"."folder" USING btree (
    "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_id_like" ON "public"."folder" USING btree (
    "id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_name" ON "public"."folder" USING btree (
    "name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_name_like" ON "public"."folder" USING btree (
    "name" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_parent_id" ON "public"."folder" USING btree (
    "parent_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_parent_id_like" ON "public"."folder" USING btree (
    "parent_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_update_time" ON "public"."folder" USING btree (
    "update_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "application_folder_user_id" ON "public"."folder" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table folder
-- ----------------------------
ALTER TABLE "public"."folder" ADD CONSTRAINT "application_folder_pkey" PRIMARY KEY ("id");




-- ----------------------------
-- Table structure for tool
-- ----------------------------
DROP TABLE IF EXISTS "public"."tool";
CREATE TABLE "public"."tool" (
                                 "create_time" timestamp(6) NOT NULL,
                                 "update_time" timestamp(6) NOT NULL,
                                 "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                 "desc" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                 "code" varchar(10240) COLLATE "pg_catalog"."default" NOT NULL,
                                 "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "is_active" bool NOT NULL,
                                 "input_field_list" jsonb NOT NULL,
                                 "tool_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                 "init_field_list" jsonb,
                                 "init_params" jsonb,
                                 "label" varchar(128) COLLATE "pg_catalog"."default",
                                 "scope" varchar(50) COLLATE "pg_catalog"."default",
                                 "icon" varchar(255) COLLATE "pg_catalog"."default",
                                 "folder_id" varchar(64) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."tool"."tool_type" IS '工具类型';

-- ----------------------------
-- Indexes structure for table tool
-- ----------------------------
CREATE INDEX "tool_user_id" ON "public"."tool" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table tool
-- ----------------------------
ALTER TABLE "public"."tool" ADD CONSTRAINT "tool_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table tool
-- ----------------------------
ALTER TABLE "public"."tool" ADD CONSTRAINT "tool_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for model
-- ----------------------------
DROP TABLE IF EXISTS "public"."model";
CREATE TABLE "public"."model" (
                                  "create_time" timestamp(6) NOT NULL,
                                  "update_time" timestamp(6) NOT NULL,
                                  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_type" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "provider" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "credential" varchar(102400) COLLATE "pg_catalog"."default" NOT NULL,
                                  "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                  "meta" jsonb NOT NULL,
                                  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_params_form" jsonb NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table model
-- ----------------------------
CREATE INDEX "model_user_id" ON "public"."model" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table model
-- ----------------------------
ALTER TABLE "public"."model" ADD CONSTRAINT "model_name_user_id_uniq" UNIQUE ("name", "user_id");

-- ----------------------------
-- Primary Key structure for table model
-- ----------------------------
ALTER TABLE "public"."model" ADD CONSTRAINT "model_pkey" PRIMARY KEY ("id");



-- ----------------------------
-- Table structure for embedding
-- ----------------------------
DROP TABLE IF EXISTS "public"."embedding";
CREATE TABLE "public"."embedding" (
                                      "id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                      "source_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                      "source_type" int2 NOT NULL,
                                      "is_active" bool NOT NULL,
                                      "embedding" "public"."vector" NOT NULL,
                                      "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "search_vector" tsvector,
                                      "dimension" int4 NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table embedding
-- ----------------------------
CREATE INDEX "embedding_knowledge_id" ON "public"."embedding" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "embedding_document_id" ON "public"."embedding" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "embedding_id_like" ON "public"."embedding" USING btree (
    "id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "embedding_paragraph_id" ON "public"."embedding" USING btree (
    "paragraph_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table embedding
-- ----------------------------
ALTER TABLE "public"."embedding" ADD CONSTRAINT "embedding_pkey" PRIMARY KEY ("id");



-- ----------------------------
-- Table structure for application_knowledge_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_knowledge_mapping";
CREATE TABLE "public"."application_knowledge_mapping" (
                                                          "create_time" timestamp(6) NOT NULL,
                                                          "update_time" timestamp(6) NOT NULL,
                                                          "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_knowledge_mapping
-- ----------------------------
CREATE INDEX "application_knowledge_mapping_application_id" ON "public"."application_knowledge_mapping" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_knowledge_mapping_knowledge_id" ON "public"."application_knowledge_mapping" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_knowledge_mapping
-- ----------------------------
ALTER TABLE "public"."application_knowledge_mapping" ADD CONSTRAINT "application_knowledge_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_knowledge_mapping
-- ----------------------------
ALTER TABLE "public"."application_knowledge_mapping" ADD CONSTRAINT "application_knowledge_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_knowledge_mapping" ADD CONSTRAINT "application_knowledge_mapping_knowledge_id_fk_knowledge_id" FOREIGN KEY ("knowledge_id") REFERENCES "public"."knowledge" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for problem_paragraph_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."problem_paragraph_mapping";
CREATE TABLE "public"."problem_paragraph_mapping" (
                                                      "create_time" timestamp(6) NOT NULL,
                                                      "update_time" timestamp(6) NOT NULL,
                                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "problem_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table problem_paragraph_mapping
-- ----------------------------
CREATE INDEX "problem_paragraph_mapping_knowledge_id" ON "public"."problem_paragraph_mapping" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "problem_paragraph_mapping_document_id" ON "public"."problem_paragraph_mapping" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "problem_paragraph_mapping_paragraph_id" ON "public"."problem_paragraph_mapping" USING btree (
    "paragraph_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "problem_paragraph_mapping_problem_id" ON "public"."problem_paragraph_mapping" USING btree (
    "problem_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table problem_paragraph_mapping
-- ----------------------------
ALTER TABLE "public"."problem_paragraph_mapping" ADD CONSTRAINT "problem_paragraph_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table problem_paragraph_mapping
-- ----------------------------
ALTER TABLE "public"."problem_paragraph_mapping" ADD CONSTRAINT "problem_paragraph_mapping_document_id_fk_document_id" FOREIGN KEY ("document_id") REFERENCES "public"."document" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
