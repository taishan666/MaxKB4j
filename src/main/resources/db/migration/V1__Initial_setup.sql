
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
                                           "create_time" timestamptz(6) NOT NULL,
                                           "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table system_setting
-- ----------------------------
ALTER TABLE "public"."system_setting" ADD CONSTRAINT "system_setting_pkey" PRIMARY KEY ("type");

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS "public"."file";
CREATE TABLE "public"."file" (
                                 "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "file_name" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                 "loid" int4 NOT NULL,
                                 "meta" jsonb NOT NULL,
                                 "create_time" timestamptz(6) NOT NULL,
                                 "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table file
-- ----------------------------
ALTER TABLE "public"."file" ADD CONSTRAINT "file_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for image
-- ----------------------------
DROP TABLE IF EXISTS "public"."image";
CREATE TABLE "public"."image" (
                                  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                  "image" bytea NOT NULL,
                                  "image_name" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                  "create_time" timestamptz(6) NOT NULL,
                                  "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table image
-- ----------------------------
ALTER TABLE "public"."image" ADD CONSTRAINT "image_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS "public"."user";
CREATE TABLE "public"."user" (
                                 "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "email" varchar(254) COLLATE "pg_catalog"."default",
                                 "phone" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                 "nick_name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "username" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "password" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "role" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                 "is_active" bool NOT NULL,
                                 "source" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
                                 "language" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
                                 "create_time" timestamptz(6),
                                 "update_time" timestamptz(6)
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
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS "public"."team";
CREATE TABLE "public"."team" (
                                 "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                 "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                 "create_time" timestamptz(6) NOT NULL,
                                 "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table team
-- ----------------------------
ALTER TABLE "public"."team" ADD CONSTRAINT "team_pkey" PRIMARY KEY ("user_id");

-- ----------------------------
-- Foreign Keys structure for table team
-- ----------------------------
ALTER TABLE "public"."team" ADD CONSTRAINT "team_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;

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
                                        "dataset_setting" jsonb NOT NULL,
                                        "model_setting" jsonb NOT NULL,
                                        "problem_optimization" bool NOT NULL,
                                        "model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "icon" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                        "type" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                        "work_flow" jsonb NOT NULL,
                                        "model_params_setting" jsonb NOT NULL,
                                        "stt_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "stt_model_enable" bool NOT NULL,
                                        "tts_model_id" varchar(50) COLLATE "pg_catalog"."default",
                                        "tts_model_enable" bool NOT NULL,
                                        "tts_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                        "problem_optimization_prompt" varchar(102400) COLLATE "pg_catalog"."default",
                                        "tts_model_params_setting" jsonb NOT NULL,
                                        "clean_time" int4 NOT NULL,
                                        "file_upload_enable" bool NOT NULL,
                                        "file_upload_setting" jsonb NOT NULL,
                                        "tts_autoplay" bool,
                                        "stt_auto_send" bool,
                                        "create_time" timestamptz(6) NOT NULL,
                                        "update_time" timestamptz(6) NOT NULL
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
-- Table structure for team_member
-- ----------------------------
DROP TABLE IF EXISTS "public"."team_member";
CREATE TABLE "public"."team_member" (
                                        "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "team_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                        "create_time" timestamptz(6) NOT NULL,
                                        "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table team_member
-- ----------------------------
CREATE INDEX "team_member_team_id_26812b86" ON "public"."team_member" USING btree (
    "team_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "team_member_user_id_9e3ae43b" ON "public"."team_member" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table team_member
-- ----------------------------
ALTER TABLE "public"."team_member" ADD CONSTRAINT "team_member_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table team_member
-- ----------------------------
ALTER TABLE "public"."team_member" ADD CONSTRAINT "team_member_team_id_26812b86_fk_team_user_id" FOREIGN KEY ("team_id") REFERENCES "public"."team" ("user_id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."team_member" ADD CONSTRAINT "team_member_user_id_9e3ae43b_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for team_member_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."team_member_permission";
CREATE TABLE "public"."team_member_permission" (
                                                   "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                   "auth_target_type" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                   "target_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                   "operate" varchar(256)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                   "member_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                   "create_time" timestamptz(6) NOT NULL,
                                                   "update_time" timestamptz(6) NOT NULL
)
;
COMMENT ON COLUMN "public"."team_member_permission"."target_id" IS '目标Id';

-- ----------------------------
-- Indexes structure for table team_member_permission
-- ----------------------------
CREATE INDEX "team_member_permission_member_id_01dc944b" ON "public"."team_member_permission" USING btree (
    "member_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table team_member_permission
-- ----------------------------
ALTER TABLE "public"."team_member_permission" ADD CONSTRAINT "team_member_permission_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table team_member_permission
-- ----------------------------
ALTER TABLE "public"."team_member_permission" ADD CONSTRAINT "team_member_permission_member_id_01dc944b_fk_team_member_id" FOREIGN KEY ("member_id") REFERENCES "public"."team_member" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for function_lib
-- ----------------------------
DROP TABLE IF EXISTS "public"."function_lib";
CREATE TABLE "public"."function_lib" (
                                         "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                         "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                         "desc" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                         "code" varchar(102400) COLLATE "pg_catalog"."default" NOT NULL,
                                         "input_field_list" jsonb NOT NULL,
                                         "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                         "is_active" bool NOT NULL,
                                         "permission_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                         "type" int2 NOT NULL,
                                         "create_time" timestamptz(6) NOT NULL,
                                         "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table function_lib
-- ----------------------------
CREATE INDEX "function_lib_user_id" ON "public"."function_lib" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table function_lib
-- ----------------------------
ALTER TABLE "public"."function_lib" ADD CONSTRAINT "function_lib_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table function_lib
-- ----------------------------
ALTER TABLE "public"."function_lib" ADD CONSTRAINT "function_lib_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;

-- ----------------------------
-- Primary Key structure for table application
-- ----------------------------
ALTER TABLE "public"."application" ADD CONSTRAINT "application_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application
-- ----------------------------
ALTER TABLE "public"."application" ADD CONSTRAINT "application_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_access_token
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_access_token";
CREATE TABLE "public"."application_access_token" (
                                                     "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "access_token" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                     "is_active" bool NOT NULL,
                                                     "access_num" int4 NOT NULL,
                                                     "white_active" bool NOT NULL,
                                                     "white_list" varchar(128)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                     "show_source" bool NOT NULL,
                                                     "language" varchar(10) COLLATE "pg_catalog"."default",
                                                     "create_time" timestamptz(6) NOT NULL,
                                                     "update_time" timestamptz(6) NOT NULL
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
ALTER TABLE "public"."application_access_token" ADD CONSTRAINT "application_access_t_application_id_d90b8cec_fk_applicati" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_api_key
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_api_key";
CREATE TABLE "public"."application_api_key" (
                                                "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "secret_key" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
                                                "is_active" bool NOT NULL,
                                                "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                "allow_cross_domain" bool NOT NULL,
                                                "cross_domain_list" varchar(128)[] COLLATE "pg_catalog"."default" NOT NULL,
                                                "create_time" timestamptz(6) NOT NULL,
                                                "update_time" timestamptz(6) NOT NULL
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
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_application_id_376d9a01_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_api_key" ADD CONSTRAINT "application_api_key_user_id_e9e85f1b_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for application_chat
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat";
CREATE TABLE "public"."application_chat" (
                                             "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "overview" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
                                             "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "client_id" varchar(50) COLLATE "pg_catalog"."default",
                                             "is_deleted" bool NOT NULL,
                                             "create_time" timestamptz(6) NOT NULL,
                                             "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_chat
-- ----------------------------
CREATE INDEX "application_chat_application_id_0c9f6b90" ON "public"."application_chat" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_chat
-- ----------------------------
ALTER TABLE "public"."application_chat" ADD CONSTRAINT "application_chat_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_chat
-- ----------------------------
ALTER TABLE "public"."application_chat" ADD CONSTRAINT "application_chat_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;

-- ----------------------------
-- Table structure for application_chat_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat_record";
CREATE TABLE "public"."application_chat_record" (
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
                                                    "answer_text_list" varchar(102400)[] NOT NULL,
                                                    "create_time" timestamptz(6) NOT NULL,
                                                    "update_time" timestamptz(6) NOT NULL
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
-- Foreign Keys structure for table application_chat_record
-- ----------------------------
ALTER TABLE "public"."application_chat_record" ADD CONSTRAINT "application_chat_record_chat_id_fk_application_chat_id" FOREIGN KEY ("chat_id") REFERENCES "public"."application_chat" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for model
-- ----------------------------
DROP TABLE IF EXISTS "public"."model";
CREATE TABLE "public"."model" (
                                  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_type" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "provider" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                  "credential" varchar(102400) COLLATE "pg_catalog"."default" NOT NULL,
                                  "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                  "meta" jsonb NOT NULL,
                                  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                  "permission_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                  "model_params_form" jsonb NOT NULL,
                                  "create_time" timestamptz(6) NOT NULL,
                                  "update_time" timestamptz(6) NOT NULL
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
-- Foreign Keys structure for table model
-- ----------------------------
ALTER TABLE "public"."model" ADD CONSTRAINT "model_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for dataset
-- ----------------------------
DROP TABLE IF EXISTS "public"."dataset";
CREATE TABLE "public"."dataset" (
                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                    "desc" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                    "type" varchar(1) COLLATE "pg_catalog"."default" NOT NULL,
                                    "meta" jsonb NOT NULL,
                                    "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "embedding_model_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "create_time" timestamptz(6) NOT NULL,
                                    "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table dataset
-- ----------------------------
CREATE INDEX "dataset_embedding_mode_id" ON "public"."dataset" USING btree (
    "embedding_model_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "dataset_user_id" ON "public"."dataset" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table dataset
-- ----------------------------
ALTER TABLE "public"."dataset" ADD CONSTRAINT "dataset_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table dataset
-- ----------------------------
ALTER TABLE "public"."dataset" ADD CONSTRAINT "dataset_embedding_mode_id_fk_model_id" FOREIGN KEY ("embedding_model_id") REFERENCES "public"."model" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."dataset" ADD CONSTRAINT "dataset_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS "public"."document";
CREATE TABLE "public"."document" (
                                     "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                     "name" varchar(150) COLLATE "pg_catalog"."default" NOT NULL,
                                     "char_length" int4 NOT NULL,
                                     "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                     "is_active" bool NOT NULL,
                                     "type" int2 NOT NULL,
                                     "meta" jsonb NOT NULL,
                                     "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                     "hit_handling_method" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                     "directly_return_similarity" float8 NOT NULL,
                                     "status_meta" jsonb NOT NULL,
                                     "create_time" timestamptz(6) NOT NULL,
                                     "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table document
-- ----------------------------
CREATE INDEX "document_dataset_id" ON "public"."document" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table document
-- ----------------------------
ALTER TABLE "public"."document" ADD CONSTRAINT "document_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table document
-- ----------------------------
ALTER TABLE "public"."document" ADD CONSTRAINT "document_dataset_id_fk_dataset_id" FOREIGN KEY ("dataset_id") REFERENCES "public"."dataset" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for paragraph
-- ----------------------------
DROP TABLE IF EXISTS "public"."paragraph";
CREATE TABLE "public"."paragraph" (
                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "content" varchar(102400) COLLATE "pg_catalog"."default" NOT NULL,
                                      "title" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                      "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                      "hit_num" int4 NOT NULL,
                                      "is_active" bool NOT NULL,
                                      "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "status_meta" jsonb,
                                      "create_time" timestamptz(6) NOT NULL,
                                      "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table paragraph
-- ----------------------------
CREATE INDEX "paragraph_dataset_id" ON "public"."paragraph" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
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
ALTER TABLE "public"."paragraph" ADD CONSTRAINT "paragraph_dataset_id_fk_dataset_id" FOREIGN KEY ("dataset_id") REFERENCES "public"."dataset" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for problem
-- ----------------------------
DROP TABLE IF EXISTS "public"."problem";
CREATE TABLE "public"."problem" (
                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "content" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                    "hit_num" int4 NOT NULL,
                                    "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "create_time" timestamptz(6) NOT NULL,
                                    "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table problem
-- ----------------------------
CREATE INDEX "problem_dataset_id" ON "public"."problem" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table problem
-- ----------------------------
ALTER TABLE "public"."problem" ADD CONSTRAINT "problem_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for problem_paragraph_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."problem_paragraph_mapping";
CREATE TABLE "public"."problem_paragraph_mapping" (
                                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "problem_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "create_time" timestamptz(6) NOT NULL,
                                                      "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table problem_paragraph_mapping
-- ----------------------------
CREATE INDEX "problem_paragraph_mapping_dataset_id" ON "public"."problem_paragraph_mapping" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
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


-- ----------------------------
-- Table structure for application_dataset_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_dataset_mapping";
CREATE TABLE "public"."application_dataset_mapping" (
                                                        "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "create_time" timestamptz(6) NOT NULL,
                                                        "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_dataset_mapping
-- ----------------------------
CREATE INDEX "application_dataset_mapping_application_id" ON "public"."application_dataset_mapping" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_dataset_mapping_dataset_id" ON "public"."application_dataset_mapping" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_dataset_mapping
-- ----------------------------
ALTER TABLE "public"."application_dataset_mapping" ADD CONSTRAINT "application_dataset_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_dataset_mapping
-- ----------------------------
ALTER TABLE "public"."application_dataset_mapping" ADD CONSTRAINT "application_dataset_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_dataset_mapping" ADD CONSTRAINT "application_dataset_mapping_dataset_id_fk_dataset_id" FOREIGN KEY ("dataset_id") REFERENCES "public"."dataset" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_public_access_client
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_public_access_client";
CREATE TABLE "public"."application_public_access_client" (
                                                             "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                             "access_num" int4 NOT NULL,
                                                             "intra_day_access_num" int4 NOT NULL,
                                                             "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                             "create_time" timestamptz(6) NOT NULL,
                                                             "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_public_access_client
-- ----------------------------
CREATE INDEX "application_public_access_client_application_id" ON "public"."application_public_access_client" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_public_access_client
-- ----------------------------
ALTER TABLE "public"."application_public_access_client" ADD CONSTRAINT "application_public_access_client_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_public_access_client
-- ----------------------------
ALTER TABLE "public"."application_public_access_client" ADD CONSTRAINT "application_public_a_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for application_work_flow_version
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_work_flow_version";
CREATE TABLE "public"."application_work_flow_version" (
                                                          "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "work_flow" jsonb NOT NULL,
                                                          "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "publish_user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "publish_user_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                          "create_time" timestamptz(6) NOT NULL,
                                                          "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_work_flow_version
-- ----------------------------
CREATE INDEX "application_work_flow_version_application_id_e1ae17fd" ON "public"."application_work_flow_version" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_work_flow_version
-- ----------------------------
ALTER TABLE "public"."application_work_flow_version" ADD CONSTRAINT "application_work_flow_version_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_work_flow_version
-- ----------------------------
ALTER TABLE "public"."application_work_flow_version" ADD CONSTRAINT "application_work_flow_application_id_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;

-- ----------------------------
-- Table structure for embedding
-- ----------------------------
DROP TABLE IF EXISTS "public"."embedding";
CREATE TABLE "public"."embedding" (
                                      "id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                      "source_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                      "source_type" varchar(5) COLLATE "pg_catalog"."default" NOT NULL,
                                      "is_active" bool NOT NULL,
                                      "embedding" "public"."vector" NOT NULL,
                                      "meta" jsonb NOT NULL,
                                      "dataset_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                      "search_vector" tsvector
)
;

-- ----------------------------
-- Indexes structure for table embedding
-- ----------------------------
CREATE INDEX "embedding_dataset_id" ON "public"."embedding" USING btree (
    "dataset_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
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
-- Table structure for mcp_lib
-- ----------------------------
DROP TABLE IF EXISTS "public"."mcp_lib";
CREATE TABLE "public"."mcp_lib" (
                                    "create_time" timestamptz(6) NOT NULL,
                                    "update_time" timestamptz(6) NOT NULL,
                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                    "desc" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                    "sse_url" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                    "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                    "is_active" bool NOT NULL,
                                    "permission_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                    "mcp_tools" jsonb
)
;

-- ----------------------------
-- Indexes structure for table mcp_lib
-- ----------------------------
CREATE INDEX "mcp_lib_user_id" ON "public"."mcp_lib" USING btree (
    "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table mcp_lib
-- ----------------------------
ALTER TABLE "public"."mcp_lib" ADD CONSTRAINT "mcp_lib_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table mcp_lib
-- ----------------------------
ALTER TABLE "public"."mcp_lib" ADD CONSTRAINT "mcp_lib_user_id_fk_user_id" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_mcp_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_mcp_mapping";
CREATE TABLE "public"."application_mcp_mapping" (
                                                    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "mcp_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                    "create_time" timestamptz(6) NOT NULL,
                                                    "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_mcp_mapping
-- ----------------------------
CREATE INDEX "application_mcp_mapping_application_id" ON "public"."application_mcp_mapping" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_mcp_mapping_mcp_id" ON "public"."application_mcp_mapping" USING btree (
    "mcp_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_mcp_mapping
-- ----------------------------
ALTER TABLE "public"."application_mcp_mapping" ADD CONSTRAINT "application_mcp_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_mcp_mapping
-- ----------------------------
ALTER TABLE "public"."application_mcp_mapping" ADD CONSTRAINT "application_mcp_mapping_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_mcp_mapping" ADD CONSTRAINT "application_mcp_mapping_fk_mcp_id" FOREIGN KEY ("mcp_id") REFERENCES "public"."mcp_lib" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;


-- ----------------------------
-- Table structure for application_platform
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_platform";
CREATE TABLE "public"."application_platform" (
                                                 "create_time" timestamptz(6) NOT NULL,
                                                 "update_time" timestamptz(6) NOT NULL,
                                                 "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                 "status" jsonb NOT NULL,
                                                 "config" jsonb NOT NULL
)
;

-- ----------------------------
-- Uniques structure for table application_platform
-- ----------------------------
ALTER TABLE "public"."application_platform" ADD CONSTRAINT "application_platform_app_id_key" UNIQUE ("application_id");

-- ----------------------------
-- Primary Key structure for table application_platform
-- ----------------------------
ALTER TABLE "public"."application_platform" ADD CONSTRAINT "application_platform_token_pkey" PRIMARY KEY ("application_id");

-- ----------------------------
-- Foreign Keys structure for table application_platform
-- ----------------------------
ALTER TABLE "public"."application_platform" ADD CONSTRAINT "application_platform_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for application_function_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_function_mapping";
CREATE TABLE "public"."application_function_mapping" (
                                                         "create_time" timestamptz(6) NOT NULL,
                                                         "update_time" timestamptz(6) NOT NULL,
                                                         "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                         "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                         "function_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_function_mapping
-- ----------------------------
CREATE INDEX "application_function_mapping_application_id" ON "public"."application_function_mapping" USING btree (
    "application_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_function_mapping_function_id" ON "public"."application_function_mapping" USING btree (
    "function_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table application_function_mapping
-- ----------------------------
ALTER TABLE "public"."application_function_mapping" ADD CONSTRAINT "application_function_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table application_function_mapping
-- ----------------------------
ALTER TABLE "public"."application_function_mapping" ADD CONSTRAINT "application_function_mapping_fk_application_id" FOREIGN KEY ("application_id") REFERENCES "public"."application" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "public"."application_function_mapping" ADD CONSTRAINT "application_mcp_mapping_fk_function_id" FOREIGN KEY ("function_id") REFERENCES "public"."function_lib" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION DEFERRABLE INITIALLY DEFERRED;



-- ----------------------------
-- Table structure for setting_model_param
-- ----------------------------
DROP TABLE IF EXISTS "public"."setting_model_param";
CREATE TABLE "public"."setting_model_param" (
                                                "id" int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY (
                                                    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1
),
                                                "label" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                "field" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                                "default_value" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
                                                "input_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
                                                "attrs" jsonb NOT NULL,
                                                "required" bool NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table setting_model_param
-- ----------------------------
ALTER TABLE "public"."setting_model_param" ADD CONSTRAINT "setting_model_param_pkey" PRIMARY KEY ("id");
