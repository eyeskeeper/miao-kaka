# 喵卡卡 · 文件存储升级（压缩预览 + 阿里云 OSS + AI 预审扩展点）设计文档

日期：2026-09-13
状态：已实现并通过自检（提交 `bf81811`；按约定 OSS 功能免在线测试，需真实凭证）
前置：一期凭证上传（`2026-09-10-phase1-core-design.md`）、死斗凭证审核流（`2026-09-11-duel-and-focus-design.md`）、视觉预审服务与缓存基建（`2026-09-12-redis-cache-design.md`）

## 1. 背景

一期遗留的文件上传只有本地磁盘单 URL 存储。本次升级三项要求：①上传同时提供原图与压缩预览图（预览图用于展示）；②AI 在图片上传时刻审核（功能先不做，预留方法）；③对象存储采用阿里云 OSS。

## 2. 决策记录（grilling 七问）

| 决策点 | 结论 |
|---|---|
| 压缩责任方 | **客户端只传原图，服务端压缩生成预览图**——单一事实来源、规格服务端可控、弱网不传双份（否决客户端传两张） |
| 预览图规格 | 最长边 ≤750px、JPEG 质量 0.7、透明背景转白底（PNG→JPEG）；原图上限维持 5MB / jpg·jpeg·png·webp |
| OSS 访问模型 | **公共读 Bucket**（一期）：URL 含随机 UUID 不可枚举，风险可控；二期隐私加固时切私有 + 签名 URL，`StorageService` 接口不变 |
| 凭证与环境 | `ALIYUN_*` 环境变量注入绝不进代码；`app.storage.type=local\|oss` 开关——默认 local，本地无凭证照常开发，线上切 OSS 只改环境变量 |
| AI 审核时机 | **上传成功那一刻预审，结论进 Redis（key 挂 URL，TTL 7 天）**；死斗凭证提交时直接消费已存结论不再二次调用；`app.ai.vision.*` 开关关闭时全链路空操作——"预留方法"即此扩展点 |
| 凭证表 | `check_in_evidence` 加 `image_preview_path`；历史数据为空时展示端回退原图 |
| 现有端点 | 升级 `POST /upload/image` 响应为 `{url, previewUrl}`（多字段向后兼容），不新增端点 |

## 3. 实现要点

- **双实现条件装配**：`LocalStorageService`（`matchIfMissing=true`）/ `OssStorageService`（`havingValue="oss"`）二选一注册为 `StorageService`；OSS 凭证不完整时**启动即失败**（快速暴露配置问题，而非运行时报错）
- **接口升级**：`storeImage` 返回 `StoredImage{url, previewUrl}`；`resolve(Path)` 删除，改为 `readAllBytes(url)`——本地读磁盘、OSS 走 SDK GetObject，AI 读图对存储实现无感
- **压缩**：`ImageUtils.generatePreview`（Thumbnailator：白底画布 + 缩放 + JPEG 0.7），local 与 oss 共用；**先压缩再落盘/上传**——预览生成失败不产生"有原图无预览"的残缺状态（自检中发现并修正的顺序缺陷）
- **AI 预审扩展点**：`EvidenceAiPreCheckService.preCheck(url)`（可用且未缓存时审一次，结论进 Redis）/ `consume(url)`（非破坏性读取）。上传接口与死斗打卡两处存储点都触发；`DuelBattleServiceImpl` 原审核时调 GLM 的逻辑改为消费预审结论，无缓存（如历史图片）时自动补审
- **视觉服务签名改造**：`review(Path, context)` → `review(byte[], mime, context)`——本地文件路径对 OSS 无意义，字节 + MIME 对任意存储通用；mime 由 `ImageUtils.mimeOf(url)` 按扩展名推断

## 4. 配置与部署

- 本地（默认）：`app.storage.type: local`，上传目录 `./data/uploads`，零配置
- 生产：`STORAGE_TYPE=oss` + `ALIYUN_OSS_ENDPOINT/BUCKET/ACCESS_KEY_ID/ACCESS_KEY_SECRET` 环境变量；compose 已透传；`.env.example` 已更新
- Nginx：OSS 模式下图片直连阿里云域名，不再走 `/uploads/**` 静态映射

## 5. 自检记录

- 编译通过、Spring 上下文正常启动（双实现条件装配无冲突）
- local 模式实际上传一次：响应返回 `{url, previewUrl}` 双 URL，磁盘同目录落盘 `xxx.png` + `xxx_preview.jpg` 双文件
- 自检中发现并修复：`storeImage` 初版"先写原图、后压预览"的顺序会在压缩失败时留下残缺状态，两实现均改为**先压缩后写**
- 按用户约定，OSS 在线功能不做测试（无真实凭证；待凭证就绪后仅需一次真机上传验证）

## 6. 明确不做（二期候选）

私有 Bucket + 签名 URL、孤儿图片生命周期清理（上传未使用的图片）、图片水印、多尺寸缩略图、客户端直传 OSS（STS 临时凭证模式）。
