# 气泡标签云 — 设计规格

日期：2026-06-20
状态：approved

## 概述

将标签云页面 (`/tag/index`) 从 Naive UI NTag 列表改造为**有机气泡云**：
- 每个标签是一个彩虹色实心圆
- 圆圈大小由文章数量决定（文章越多越大）
- 点击进入对应标签详情页

## 视觉规格

| 属性 | 值 |
|------|-----|
| 圆圈大小范围 | min 72px，max 160px（桌面端） |
| 大小计算 | 对数缩放：`size = min + (max - min) * (log(count) / log(maxCount))` |
| 颜色数量 | 8 种 |
| 颜色分配 | 确定性：`palette[tag.id % 8]` |
| 圆圈样式 | 实心圆，微渐变（中心亮→边缘稍深） |
| 随机倾斜 | ±3°，seed=tag.id，渲染时确定不变 |
| 悬停效果 | scale(1.15) + 阴影抬升 + 过渡 200ms |
| 内容排版 | 标签名居中大号，文章数下方小字 |
| 响应式 | 小于 640px 时圆圈整体缩小 0.7× |

### 颜色调色板

```
#FF6B6B  暖红
#FF9F43  暖橙
#FECA57  金黄
#54A0FF  天蓝
#5F27CD  深紫
#01A3A4  青绿
#2ED573  翠绿
#FF6FB7  粉红
```

### 边缘情况

| 场景 | 行为 |
|------|------|
| 标签为 0 个 | 显示 EmptyState |
| 标签只有 1 个 | 单个圆圈居中 |
| 标签很多（>30） | 自然换行，小圆圈环绕大圆圈 |
| 所有标签 articleCount 相同 | 全部同样大小 |
| 无法加载 | 显示 NResult error + 重试按钮 |

## 技术方案

### 改动范围

**唯一改动文件**：`blog-frontend/pages/tag/index.vue`

- 模板：`<NTag>` 循环替换为 `<div>` 气泡循环
- 脚本：添加调色板、大小计算、旋转计算逻辑
- 删除 `import { NTag } from 'naive-ui'`

**不动**：
- `api/modules/tag.ts` — API 不变
- `types/index.ts` — `TagCloudItem` 不变
- `pages/tag/[id].vue` — 详情页不变
- `EmptyState`、`NResult` — 组件复用

### 布局方式

CSS flex-wrap +`justify-center` + `items-center`。每个圆圈通过 `style` 绑定：
- `width/height` — 计算后的大小
- `background` — radial-gradient 微渐变
- `transform` — rotate(±3°) + hover 时 scale(1.15)
- `cursor: pointer` — 点击导航

### 数据流

```
组件挂载 → getTagCloud('count') → TagCloudItem[] (含 articleCount)
→ 计算 maxCount → 每个 tag 计算 size/color/rotation
→ 渲染气泡网格
→ 用户点击 → router.push(`/tag/${tag.id}`)
```

## 验收标准

1. 标签云页面显示彩色圆圈，不是 Naive UI 标签
2. 文章数多的标签圆圈明显更大
3. 每个标签颜色不同且固定（刷新不变）
4. 圆圈有轻微随机倾斜
5. 鼠标悬停圆圈放大 + 阴影
6. 圆圈内显示标签名和文章数
7. 点击圆圈跳转到 `/tag/[id]`
8. 移动端圆圈等比缩小，不溢出
