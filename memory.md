# Memory.md

## 项目概况
- **项目名称**: common-lib
- **类型**: Java 公共库（多模块 Maven 项目）
- **技术栈**: Java 17, Spring Boot 3.x, Lombok
- **构建工具**: Maven
- **模块数量**: 25+

## 架构设计

### 模块划分

| 模块 | 职责 |
| --- | --- |
| common-core | 核心工具类、基础组件 |
| common-storage | 文件存储（Local/MinIO/OSS） |
| common-web-notify | SSE 推送 |
| common-cache | 缓存抽象 |
| common-mq-* | 消息队列（Kafka/Rabbit/RocketMQ） |
| common-security-* | 安全认证（JWT/SA） |
| common-data-* | 数据访问（JPA/Mongo/Search） |
| common-ai | AI 相关工具 |
| common-util | 通用工具类 |

## 核心组件

### Storage 链式 API
- **职责**: 统一文件存储接口，支持 Local/MinIO/OSS，替代 FileService
- **入口**: `Storage.bucket(name).object(key)`
- **关键方法**:
  - `write(String/byte[]/InputStream/MultipartFile)` - 写入（覆盖/新建）
  - `append(String/byte[])` - 追加
  - `read()` / `stream()` / `download()` - 读取
  - `delete()` / `copy()` / `presignedUrl()` - 其他操作
- **实现类**: LocalChainStorage, MinioChainStorage, OssChainStorage
- **数据库同步**: 所有写操作自动同步 `sys_storage_file` 表
- **元数据管理**: 自动提取文件大小、扩展名、路径等信息

## 进行中的工作
- **当前任务**: Storage 架构重构完成
- **完成度**: 100%
- **已完成**:
  - VFS 重复上传文件改为覆盖而非报错
  - Storage API 新增数据库同步功能
  - 抽取 ChainStorage 共同代码到 AbstractChainStorage 父类
  - 移动 ChainStorageService 接口到 chain 包
  - LocalChainStorage/MinioChainStorage/OssChainStorage 继承父类
  - 所有操作同步 sys_storage_file 表
  - 支持文件大小、扩展名、路径等元数据自动提取
- **下一步**: FileService 标记为 @Deprecated，逐步迁移到 Storage API

## 已知问题清单
| ID | 现象 | P级 | 影响范围 | 方案 | 状态 |
| --- | --- | --- | --- | --- | --- |
| - | - | - | - | - | - |

## 编码约定
- 链式 API 采用嵌套 Builder 模式
- 接口层方法语义化命名（write vs upload）
- 实现类直接操作字节，避免中间转换

---
**2026-02-25 UPDATED**: Storage API 数据库同步完成，为替代 FileService 做准备
