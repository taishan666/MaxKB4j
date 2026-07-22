ALTER TABLE "public"."application_chat_record"
ALTER COLUMN "problem_text" TYPE text COLLATE "pg_catalog"."default" USING "problem_text"::text,
  ALTER COLUMN "answer_text" TYPE text COLLATE "pg_catalog"."default" USING "answer_text"::text;