DROP TABLE IF EXISTS "public"."resource_mapping";
CREATE TABLE "public"."resource_mapping" (
                                             "create_time" timestamp(6) NOT NULL,
                                             "update_time" timestamp(6) NOT NULL,
                                             "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "source_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "target_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "source_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                             "target_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                             "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                             "resource_name" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Indexes structure for table resource_mapping
-- ----------------------------
CREATE INDEX "resource_mapping_create_time_bda4e392" ON "public"."resource_mapping" USING btree (
    "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_id_4ea1216b" ON "public"."resource_mapping" USING btree (
    "source_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_id_4ea1216b_like" ON "public"."resource_mapping" USING btree (
    "source_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_type_65569047" ON "public"."resource_mapping" USING btree (
    "source_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_source_type_65569047_like" ON "public"."resource_mapping" USING btree (
    "source_type" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_id_6e1f2685" ON "public"."resource_mapping" USING btree (
    "target_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_id_6e1f2685_like" ON "public"."resource_mapping" USING btree (
    "target_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_type_85c62bbb" ON "public"."resource_mapping" USING btree (
    "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_target_type_85c62bbb_like" ON "public"."resource_mapping" USING btree (
    "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "resource_mapping_update_time_1c3672d3" ON "public"."resource_mapping" USING btree (
    "update_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table resource_mapping
-- ----------------------------
ALTER TABLE "public"."resource_mapping" ADD CONSTRAINT "resource_mapping_pkey" PRIMARY KEY ("id");
