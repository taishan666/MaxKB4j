# 贡献指南 | Contributing Guide

感谢你对 MaxKB4j 的关注！欢迎通过 Issue、Pull Request、文档改进、社区传播等各种方式参与贡献。

## 反馈问题与建议

- 中文社区（主渠道）：[Gitee Issues](https://gitee.com/taisan/MaxKB4j/issues)
- 国际社区：[GitHub Issues](https://github.com/taishan666/MaxKB4j/issues)
- 提交缺陷前请先搜索已有 Issue，避免重复；描述请包含部署方式、版本号、数据库版本与复现步骤。

## 开发环境准备

- JDK 21+
- Maven 3.6+（仓库未附带 wrapper）
- PostgreSQL 12+（启用 pgvector 扩展）
- MongoDB 6.0+

### 启动依赖服务

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 本地构建与测试

```bash
# 全量构建（含单元测试）
mvn -B -ntp verify

# 跳过测试打包
mvn clean package -DskipTests
java -jar maxkb4j-start/target/maxkb4j-start.jar
```

配置文件位于 `maxkb4j-start/src/main/resources/application-{dev,prod}.yml`，通过
`--spring.profiles.active=dev` 切换。测试均为纯单元测试（不依赖数据库），可直接在 CI / 本地运行。

## 分支模型与 PR 流程

1. Fork 仓库并拉取到本地
2. 从 `dev` 分支创建特性分支：`git checkout -b feat/your-feature dev`
3. 开发完成、`mvn verify` 通过后推送到你的仓库
4. 向 `dev` 分支提交 Pull Request（Gitee 或 GitHub 均可），填写 PR 模板
5. 通过审查后合并，版本发布时统一合入 `master`

## 编码规范

- 遵循阿里巴巴 Java 开发规范，详见 [docs/编码约定.md](docs/编码约定.md)
- 新增公共能力请同步补充单元测试（JUnit 5 + AssertJ + Mockito，参考 `maxkb4j-common/src/test`）
- 涉及用户可见行为变更时更新 `CHANGELOG.md`（Keep a Changelog 格式）

## 提交信息规范

使用 Conventional Commits：`feat(module): ...` / `fix(module): ...` / `refactor(module): ...` / `docs: ...`

## 行为准则

参与本项目即代表你同意遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

---

## Contributing (English summary)

- **Issues**: [Gitee](https://gitee.com/taisan/MaxKB4j/issues) (primary) or [GitHub](https://github.com/taishan666/MaxKB4j/issues)
- **Setup**: JDK 21+, Maven 3.6+, PostgreSQL 12+ (pgvector), MongoDB 6.0+; start deps via `docker compose -f docker-compose.dev.yml up -d`
- **Build & test**: `mvn -B -ntp verify` (all tests are plain unit tests, no database required)
- **PR flow**: fork → branch off `dev` (`feat/...`) → `mvn verify` passes → open a PR against `dev`
- **Style**: Alibaba Java coding guidelines ([docs/编码约定.md](docs/编码约定.md), Chinese); Conventional Commits; add unit tests and update `CHANGELOG.md` for user-visible changes
