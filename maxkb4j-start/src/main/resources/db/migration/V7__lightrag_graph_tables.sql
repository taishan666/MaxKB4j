-- ----------------------------
-- Table structure for graph_entity
-- ----------------------------
CREATE TABLE "public"."graph_entity" (
    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "name" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
    "entity_type" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "description" varchar(4096) COLLATE "pg_catalog"."default",
    "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "is_active" bool NOT NULL DEFAULT true,
    "embedding" "public"."vector",
    "dimension" int4 NOT NULL,
    "create_time" timestamp(6) NOT NULL,
    "update_time" timestamp(6) NOT NULL
);

-- ----------------------------
-- Indexes structure for table graph_entity
-- ----------------------------
CREATE INDEX "graph_entity_knowledge_id" ON "public"."graph_entity" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_entity_document_id" ON "public"."graph_entity" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_entity_name_like" ON "public"."graph_entity" USING btree (
    "name" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table graph_entity
-- ----------------------------
ALTER TABLE "public"."graph_entity" ADD CONSTRAINT "graph_entity_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for graph_relationship
-- ----------------------------
CREATE TABLE "public"."graph_relationship" (
    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "source_entity_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "target_entity_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "description" varchar(4096) COLLATE "pg_catalog"."default" NOT NULL,
    "keywords" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
    "weight" float8 NOT NULL DEFAULT 1.0,
    "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "is_active" bool NOT NULL DEFAULT true,
    "embedding" "public"."vector",
    "dimension" int4 NOT NULL,
    "create_time" timestamp(6) NOT NULL,
    "update_time" timestamp(6) NOT NULL
);

-- ----------------------------
-- Indexes structure for table graph_relationship
-- ----------------------------
CREATE INDEX "graph_relationship_knowledge_id" ON "public"."graph_relationship" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_relationship_document_id" ON "public"."graph_relationship" USING btree (
    "document_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_relationship_source_entity_id" ON "public"."graph_relationship" USING btree (
    "source_entity_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_relationship_target_entity_id" ON "public"."graph_relationship" USING btree (
    "target_entity_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table graph_relationship
-- ----------------------------
ALTER TABLE "public"."graph_relationship" ADD CONSTRAINT "graph_relationship_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for graph_entity_paragraph_mapping
-- ----------------------------
CREATE TABLE "public"."graph_entity_paragraph_mapping" (
    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "entity_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "create_time" timestamp(6) NOT NULL,
    "update_time" timestamp(6) NOT NULL
);

-- ----------------------------
-- Indexes structure for table graph_entity_paragraph_mapping
-- ----------------------------
CREATE INDEX "graph_entity_paragraph_mapping_entity_id" ON "public"."graph_entity_paragraph_mapping" USING btree (
    "entity_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_entity_paragraph_mapping_paragraph_id" ON "public"."graph_entity_paragraph_mapping" USING btree (
    "paragraph_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_entity_paragraph_mapping_knowledge_id" ON "public"."graph_entity_paragraph_mapping" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table graph_entity_paragraph_mapping
-- ----------------------------
ALTER TABLE "public"."graph_entity_paragraph_mapping" ADD CONSTRAINT "graph_entity_paragraph_mapping_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for graph_relationship_paragraph_mapping
-- ----------------------------
CREATE TABLE "public"."graph_relationship_paragraph_mapping" (
    "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "relationship_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "paragraph_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "knowledge_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "document_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
    "create_time" timestamp(6) NOT NULL,
    "update_time" timestamp(6) NOT NULL
);

-- ----------------------------
-- Indexes structure for table graph_relationship_paragraph_mapping
-- ----------------------------
CREATE INDEX "graph_relationship_paragraph_mapping_relationship_id" ON "public"."graph_relationship_paragraph_mapping" USING btree (
    "relationship_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_relationship_paragraph_mapping_paragraph_id" ON "public"."graph_relationship_paragraph_mapping" USING btree (
    "paragraph_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );
CREATE INDEX "graph_relationship_paragraph_mapping_knowledge_id" ON "public"."graph_relationship_paragraph_mapping" USING btree (
    "knowledge_id" COLLATE "pg_catalog"."default" "pg_catalog"."varchar_pattern_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table graph_relationship_paragraph_mapping
-- ----------------------------
ALTER TABLE "public"."graph_relationship_paragraph_mapping" ADD CONSTRAINT "graph_relationship_paragraph_mapping_pkey" PRIMARY KEY ("id");