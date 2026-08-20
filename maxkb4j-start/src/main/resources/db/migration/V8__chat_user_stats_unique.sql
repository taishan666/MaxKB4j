-- ----------------------------
-- application_chat_user_stats 去重并添加唯一约束（chat_user_id + application_id）
-- 背景：visitCountOver 曾为"先查后插"，并发下同一用户会重复建行、计数丢失、限流失准。
-- 本迁移先合并存量重复行（计数累加到创建时间最早的保留行），再添加唯一索引，
-- 供代码侧使用 ON CONFLICT DO NOTHING 原子插入与 UPDATE ... +1 原子自增。
-- 说明：chat_user_id 允许 NULL（匿名场景），PostgreSQL 唯一索引中 NULL 互不相等，不影响匿名行。
-- ----------------------------

-- 1) 将重复行的计数累加到创建时间最早的保留行（create_time 相同则按 id 排序）
WITH ranked AS (
    SELECT id,
           chat_user_id,
           application_id,
           ROW_NUMBER() OVER (
               PARTITION BY chat_user_id, application_id
               ORDER BY create_time, id
           ) AS rn,
           access_num,
           intra_day_access_num
    FROM application_chat_user_stats
    WHERE chat_user_id IS NOT NULL
)
UPDATE application_chat_user_stats keeper
SET access_num           = keeper.access_num + dup.extra_access_num,
    intra_day_access_num = keeper.intra_day_access_num + dup.extra_intra_day_access_num
FROM (
    SELECT MAX(CASE WHEN rn = 1 THEN id END)                          AS keeper_id,
           chat_user_id,
           application_id,
           SUM(CASE WHEN rn > 1 THEN access_num ELSE 0 END)           AS extra_access_num,
           SUM(CASE WHEN rn > 1 THEN intra_day_access_num ELSE 0 END) AS extra_intra_day_access_num
    FROM ranked
    GROUP BY chat_user_id, application_id
    HAVING COUNT(*) > 1
) dup
WHERE keeper.id = dup.keeper_id;

-- 2) 删除已合并的重复行
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY chat_user_id, application_id
               ORDER BY create_time, id
           ) AS rn
    FROM application_chat_user_stats
    WHERE chat_user_id IS NOT NULL
)
DELETE FROM application_chat_user_stats
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

-- 3) 添加唯一索引
CREATE UNIQUE INDEX "application_chat_user_stats_user_app_uk"
    ON "public"."application_chat_user_stats" USING btree (
        "chat_user_id" COLLATE "pg_catalog"."default",
        "application_id" COLLATE "pg_catalog"."default"
    );