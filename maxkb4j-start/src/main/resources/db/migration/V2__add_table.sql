-- ----------------------------
-- Table structure for application_access
-- ----------------------------
DROP TABLE IF EXISTS "public"."application_access";
CREATE TABLE "public"."application_access" (
                                               "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                               "status" jsonb,
                                               "config" jsonb,
                                               "create_time" timestamp(6) NOT NULL,
                                               "update_time" timestamp(6) NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table application_access
-- ----------------------------
ALTER TABLE "public"."application_access" ADD CONSTRAINT "application_access_pkey" PRIMARY KEY ("id");
