# V2 转换器目录

此目录用于存放 V2 版本的实体-DTO 转换器。

## 说明

- V2 转换器负责将 V2 实体（`entity/v2/`）与统一 DTO（`model/`）之间进行双向转换
- 每个 V2 转换器需实现 `Converter<E, D>` 接口
- V2 转换器在 `DataSourceStrategyImpl` 将模块版本切换为 V2 时自动生效
- 当前阶段（Phase 1）仅建立目录结构，V2 转换器将在后续阶段实现
