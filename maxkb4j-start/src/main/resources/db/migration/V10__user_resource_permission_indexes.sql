-- ----------------------------
-- user_resource_permission 添加复合索引
-- 背景：该表仅有 id 主键，按 target_id / user_id 过滤的查询（资源成员分页、
-- 用户资源分页、权限更新前清理、getTargetIds 等）均为全表扫描，
-- 授权记录增长后 resource_user_permission 分页接口明显变慢。
-- workspace_id 当前恒为 'default' 无选择性，不纳入索引列。
-- ----------------------------

-- 资源维度：资源成员分页 / 资源权限更新 / 按资源删除
CREATE INDEX "user_resource_permission_target_type_idx"
    ON "public"."user_resource_permission" USING btree ("target_id" ASC, "auth_target_type" ASC);

-- 用户维度：用户资源分页 / 用户权限更新 / 按用户删除 / getTargetIds
CREATE INDEX "user_resource_permission_user_type_idx"
    ON "public"."user_resource_permission" USING btree ("user_id" ASC, "auth_target_type" ASC);
