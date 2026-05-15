ALTER TABLE application
    ADD COLUMN "long_term_enable" bool DEFAULT false;
ALTER TABLE application_version
    ADD COLUMN "long_term_enable" bool DEFAULT false;
-- ----------------------------
-- Table structure for application_long_term_memory
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_long_term_memory";
CREATE TABLE "public"."application_long_term_memory" (
                                                         "create_time" timestamptz(6) NOT NULL,
                                                         "update_time" timestamptz(6) NOT NULL,
                                                         "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                         "chat_user_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                                         "memory" text COLLATE "pg_catalog"."default" NOT NULL,
                                                         "application_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table application_long_term_memory
-- ----------------------------
CREATE INDEX "application_long_term_memory_application_id_chat_user_id_idx" ON "public"."application_long_term_memory" USING btree (
    "application_id" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST,
    "chat_user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "application_long_term_memory_application_id" ON "public"."application_long_term_memory" USING btree (
    "application_id" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_long_term_memory_chat_user_id" ON "public"."application_long_term_memory" USING btree (
    "chat_user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "application_long_term_memory_chat_user_id_like" ON "public"."application_long_term_memory" USING btree (
    "chat_user_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "application_long_term_memory_create_time" ON "public"."application_long_term_memory" USING btree (
    "create_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );
CREATE INDEX "application_long_term_memory_update_time" ON "public"."application_long_term_memory" USING btree (
    "update_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table application_long_term_memory
-- ----------------------------
ALTER TABLE "public"."application_long_term_memory" ADD CONSTRAINT "application_long_term_me_application_id_chat_user_uniq" UNIQUE ("application_id", "chat_user_id");

-- ----------------------------
-- Primary Key structure for table application_long_term_memory
-- ----------------------------
ALTER TABLE "public"."application_long_term_memory" ADD CONSTRAINT "application_long_term_memory_pkey" PRIMARY KEY ("id");



