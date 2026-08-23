-- 回填应用表 folder_id 为 NULL 的存量数据为默认文件夹 'default'
UPDATE "public"."application" SET "folder_id" = 'default' WHERE "folder_id" IS NULL;
