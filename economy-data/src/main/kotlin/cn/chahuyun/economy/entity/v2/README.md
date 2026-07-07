# V2 实体目录

此目录用于存放 V2 版本的数据库实体。

## 说明

- V2 实体是面向新数据模型设计的 JPA/Hibernate 实体
- V2 实体通过对应的 V2 转换器（`converter/v2/`）与统一 DTO 进行转换
- 代理器（`proxy/module/`）根据 `DataSourceStrategyImpl` 的配置自动选择 V1 或 V2 实体
- 当前阶段（Phase 1）仅建立目录结构，V2 实体将在后续阶段实现
