
-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."tag";
CREATE TABLE "public"."tag" (
                                "create_time" timestamptz(6) NOT NULL,
                                "update_time" timestamptz(6) NOT NULL,
                                "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                "key" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                "value" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table tag
-- ----------------------------
CREATE INDEX "tag_create_time_36d78b7c" ON "public"."tag" USING btree (
    "create_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_key_08a35336" ON "public"."tag" USING btree (
    "key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_key_08a35336_like" ON "public"."tag" USING btree (
    "key" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_knowled_cba590_idx" ON "public"."tag" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
    "key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_knowledge_id_5816b5de" ON "public"."tag" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_update_time_928c2700" ON "public"."tag" USING btree (
    "update_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_value_0d236690" ON "public"."tag" USING btree (
    "value" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "tag_value_0d236690_like" ON "public"."tag" USING btree (
    "value" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table tag
-- ----------------------------
ALTER TABLE "public"."tag" ADD CONSTRAINT "tag_knowledge_id_key_value_eb5f0e5d_uniq" UNIQUE ("knowledge_id", "key", "value");

-- ----------------------------
-- Primary Key structure for table tag
-- ----------------------------
ALTER TABLE "public"."tag" ADD CONSTRAINT "tag_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for document_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."document_tag";
CREATE TABLE "public"."document_tag" (
                                         "create_time" timestamptz(6) NOT NULL,
                                         "update_time" timestamptz(6) NOT NULL,
                                         "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                         "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                         "tag_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Indexes structure for table document_tag
-- ----------------------------
CREATE INDEX "document_tag_create_time_4ef5590c" ON "public"."document_tag" USING btree (
    "create_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );
CREATE INDEX "document_tag_document_id_953cb93d" ON "public"."document_tag" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "document_tag_tag_id_acaa7b3b" ON "public"."document_tag" USING btree (
    "tag_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "document_tag_update_time_2c460bdf" ON "public"."document_tag" USING btree (
    "update_time" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table document_tag
-- ----------------------------
ALTER TABLE "public"."document_tag" ADD CONSTRAINT "document_tag_document_id_tag_id_55f500a6_uniq" UNIQUE ("document_id", "tag_id");

-- ----------------------------
-- Primary Key structure for table document_tag
-- ----------------------------
ALTER TABLE "public"."document_tag" ADD CONSTRAINT "document_tag_pkey" PRIMARY KEY ("id");
