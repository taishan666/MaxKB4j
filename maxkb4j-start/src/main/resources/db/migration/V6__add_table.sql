

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
