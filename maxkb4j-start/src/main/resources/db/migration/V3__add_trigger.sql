-- ----------------------------
-- Table structure for event_trigger
-- ----------------------------
DROP TABLE IF EXISTS "public"."event_trigger";
CREATE TABLE "public"."event_trigger" (
                                          "create_time" timestamptz(6) NOT NULL,
                                          "update_time" timestamptz(6) NOT NULL,
                                          "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                          "workspace_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                          "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                          "desc" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
                                          "trigger_type" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                          "trigger_setting" jsonb NOT NULL,
                                          "meta" jsonb NOT NULL,
                                          "is_active" bool NOT NULL,
                                          "user_id" varchar(50) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Primary Key structure for table event_trigger
-- ----------------------------
ALTER TABLE "public"."event_trigger" ADD CONSTRAINT "event_trigger_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for event_trigger_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."event_trigger_task";
CREATE TABLE "public"."event_trigger_task" (
                                               "create_time" timestamptz(6) NOT NULL,
                                               "update_time" timestamptz(6) NOT NULL,
                                               "id" uuid NOT NULL,
                                               "source_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                               "source_id" uuid NOT NULL,
                                               "is_active" bool NOT NULL,
                                               "parameter" jsonb NOT NULL,
                                               "meta" jsonb NOT NULL,
                                               "trigger_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table event_trigger_task
-- ----------------------------
ALTER TABLE "public"."event_trigger_task" ADD CONSTRAINT "event_trigger_task_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for event_trigger_task_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."event_trigger_task_record";
CREATE TABLE "public"."event_trigger_task_record" (
                                                      "create_time" timestamptz(6) NOT NULL,
                                                      "update_time" timestamptz(6) NOT NULL,
                                                      "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "source_type" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "source_id" uuid NOT NULL,
                                                      "task_record_id" uuid NOT NULL,
                                                      "meta" jsonb NOT NULL,
                                                      "state" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "run_time" float8 NOT NULL,
                                                      "trigger_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                                      "trigger_task_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table event_trigger_task_record
-- ----------------------------
ALTER TABLE "public"."event_trigger_task_record" ADD CONSTRAINT "event_trigger_task_record_pkey" PRIMARY KEY ("id");
