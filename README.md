# 三国文化主题网站

> 基于 Spring Boot + Vue 3 的三国文化知识分享平台，集成 AI 智能问答、人物关系图谱、事件时空地图等核心功能。

## 项目结构

```
├── backend/          # 后端服务（Spring Boot）
├── frontend/         # 前端应用（Vue 3 + Element Plus）
├── database/         # 数据库初始化脚本
└── README.md         # 项目说明文档
```

## 技术栈

| 分类 | 技术 | 版本 |
|-----|------|-----|
| 后端框架 | Spring Boot | 3.2.x |
| 前端框架 | Vue | 3.4.x |
| 数据库 | MySQL | 8.0+ |
| 大语言模型 | 智谱 GLM-4-Flash | - |
| 可视化 | ECharts | 5.x |
| 认证授权 | JWT | - |

## 核心功能

### 1. 文化资源展示
- 人物库：三国主要人物信息展示
- 事件库：历史事件时间线
- 史料文献：三国志、三国演义等经典文献
- 典故传说：三国文化典故收录

### 2. AI 智能问答
- 基于 RAG 技术的智能问答
- 支持自然语言交互
- 多轮对话能力
- 知识库基于《三国志》《三国演义》等权威资料

### 3. 可视化展示
- **人物关系图谱**：力导向布局展示人物关联
- **事件时空地图**：时间轴与地理分布联动展示

### 4. 用户社区
- 文章发布与审核
- 论坛讨论区
- 评论与互动

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20+
- MySQL 8.0+

### 1. 数据库配置

```bash
# 创建数据库
CREATE DATABASE sanguo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入初始化脚本
mysql -u root -p sanguo < database/schema.sql
```

### 2. 后端启动

```bash
cd backend
mvn spring-boot:run
```

服务启动后访问：http://localhost:8080/api

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:5173

## API 接口

| 模块 | 接口示例 | 说明 |
|-----|---------|-----|
| 人物 | `/api/person/list` | 获取人物列表 |
| 事件 | `/api/event/list` | 获取事件列表 |
| AI问答 | `/api/ai/ask` | 向AI提问 |
| 用户 | `/api/auth/login` | 用户登录 |

## 项目特色

- 🎯 **AI 驱动**：集成大语言模型，提供智能问答服务
- 📊 **可视化展示**：人物关系图谱与时空地图双重呈现
- 📚 **权威知识库**：基于正史与文学经典构建
- 🔐 **权限管理**：完善的用户认证与授权体系
- 📱 **响应式设计**：支持多端访问

## 目录说明

```
backend/
├── src/main/java/com/sanguo/    # Java 源代码
├── src/main/resources/          # 配置文件
└── pom.xml                      # Maven 依赖

frontend/
├── src/                         # Vue 组件与页面
├── public/                      # 静态资源
└── package.json                 # npm 依赖

database/
└── schema.sql                   # 数据库初始化脚本
```

## 开发说明

- 后端服务端口：8080
- 前端开发端口：5173
- 数据库端口：3306

## 许可证

MIT License

---

*三国文化主题网站 - 传承经典，智启未来*
