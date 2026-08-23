# Flyway 数据库迁移说明

## 当前接管策略

- 已有且包含业务表的 `house_rental` 数据库：首次启动时由 `baseline-on-migrate` 登记为版本 `1`，不会重复执行 `V1__baseline_schema.sql`。
- 全新的空数据库：启动时执行 `V1__baseline_schema.sql` 创建完整表结构。
- 根目录 `house_rental.sql` 继续作为演示数据和手工初始化参考，不属于自动迁移目录。

第一次在已有数据库启用 Flyway 前，应先备份数据库。启动成功后，数据库中会新增 `flyway_schema_history` 表，这是正常现象。

## 后续迁移规则

任何表结构变更都新增迁移文件，不修改已经执行过的迁移：

```text
V2__create_agent_conversation_tables.sql
V3__add_trace_columns.sql
```

文件必须放在 `src/main/resources/db/migration`，版本号递增，描述使用小写英文和下划线。生产环境禁止执行 `flyway clean`。

如需临时停用 Flyway，可在启动环境中设置：

```text
FLYWAY_ENABLED=false
```
