DROP TABLE IF EXISTS "public"."resource_mapping";
CREATE TABLE "public"."resource_mapping" (
                                             "create_time" timestamp(6) NOT NULL,
                                             "update_time" timestamp(6) NOT NULL,
                                             "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "source_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "target_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "source_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                             "target_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table resource_mapping
-- ----------------------------
CREATE INDEX "resource_mapping_create_time" ON "public"."resource_mapping" USING btree (
    "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_id" ON "public"."resource_mapping" USING btree (
    "source_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_id_like" ON "public"."resource_mapping" USING btree (
    "source_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_type" ON "public"."resource_mapping" USING btree (
    "source_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_type_like" ON "public"."resource_mapping" USING btree (
    "source_type" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_id" ON "public"."resource_mapping" USING btree (
    "target_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_id_like" ON "public"."resource_mapping" USING btree (
    "target_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_type" ON "public"."resource_mapping" USING btree (
    "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_type_like" ON "public"."resource_mapping" USING btree (
    "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_update_time" ON "public"."resource_mapping" USING btree (
    "update_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table resource_mapping
-- ----------------------------
ALTER TABLE "public"."resource_mapping" ADD CONSTRAINT "resource_mapping_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for application_chat_share_link
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_chat_share_link";
CREATE TABLE "public"."application_chat_share_link" (
                                                        "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                        "share_type" varchar(25) COLLATE "pg_catalog"."default",
                                                        "chat_record_ids" varchar[] COLLATE "pg_catalog"."default",
                                                        "application_id" varchar(50) COLLATE "pg_catalog"."default",
                                                        "chat_id" varchar(50) COLLATE "pg_catalog"."default",
                                                        "user_id" varchar(50) COLLATE "pg_catalog"."default",
                                                        "create_time" timestamptz(6) NOT NULL,
                                                        "update_time" timestamptz(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table application_chat_share_link
-- ----------------------------
ALTER TABLE "public"."application_chat_share_link" ADD CONSTRAINT "application_chat_share_link_pkey" PRIMARY KEY ("id");
