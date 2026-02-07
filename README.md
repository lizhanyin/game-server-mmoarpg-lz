# 游戏服务器引擎

多进程，多线程，实现了分布式，集群，能应付大型网络游戏的高并发计算

## 核心技术栈

``` xml
  gof
   |--fastjson        # JSON处理
   |--netty           # 网络通信框架
   |--zmq             # 消息队列
   |--protocal-buffer # 协议序列化
   |--java25          # Java 25特性
```

## 项目结构

| 工程 | 说明 |
|------|------|
| `lzGatewaySrv` | 网关服务 - 统一接入层 |
| `lzWorldSrv` | 主工程 - 游戏世界服务 |
| `robot` | 机器人工程 - 压力测试 |
| `gof` | 核心框架 - 基础组件 |

---

# lzGatewaySrv 网关服务架构

## 概述

独立网关服务，作为客户端和游戏服务之间的统一接入层，提供连接管理、协议适配、安全防护、路由分发和跨服功能。

## 架构图

```
                        ┌─────────────────────────────────────┐
                        │          客户端                      │
                        │   (TCP / WebSocket / HTTP)          │
                        └───────────────┬─────────────────────┘
                                        │
                        ┌───────────────▼─────────────────────┐
                        │         Gateway Service             │
                        │  ┌───────────────────────────────┐  │
                        │  │  连接管理                       │  │
                        │  │  • ConnectionManager          │  │
                        │  ├───────────────────────────────┤  │
                        │  │  协议适配                       │  │
                        │  │  • TcpHandler                  │  │
                        │  │  • WebSocketHandler            │  │
                        │  │  • HttpHandler                 │  │
                        │  ├───────────────────────────────┤  │
                        │  │  安全防护                       │  │
                        │  │  • BlackWhiteList              │  │
                        │  │  • RateLimiter                 │  │
                        │  │  • SignatureChecker            │  │
                        │  │  • AntiCheatDetector           │  │
                        │  ├───────────────────────────────┤  │
                        │  │  路由分发                       │  │
                        │  │  • ServiceRouter               │  │
                        │  │  • LoadBalancer                │  │
                        │  │  • ServiceDiscovery            │  │
                        │  ├───────────────────────────────┤  │
                        │  │  跨服功能                       │  │
                        │  │  • MatchService                │  │
                        │  │  • TeamService                 │  │
                        │  │  • RoomManager                 │  │
                        │  └───────────────────────────────┘  │
                        └───────────────┬─────────────────────┘
                                        │ 内部RPC
            ┌───────────────────────────┼───────────────────────────┐
            │                           │                           │
┌───────────▼───────────┐  ┌──────────▼──────────┐  ┌───────────▼──────────┐
│    World Service      │  │   Battle Service    │  │    Match Service     │
│  (玩家、社交、交易)    │  │   (战斗场景)        │  │   (跨服匹配)          │
└───────────────────────┘  └─────────────────────┘  └──────────────────────┘
```

## 目录结构

```
lzGatewaySrv/
├── src/org/gof/demo/gatewaysrv/
│   ├── connection/        # 连接管理
│   │   ├── Connection.java
│   │   ├── ConnectionState.java
│   │   └── ConnectionManager.java
│   ├── protocol/          # 协议适配
│   │   ├── ProtocolAdapter.java
│   │   ├── TcpHandler.java
│   │   ├── WebSocketHandler.java
│   │   └── HttpHandler.java
│   ├── security/          # 安全防护
│   │   ├── BlackWhiteList.java
│   │   ├── RateLimiter.java
│   │   ├── SignatureChecker.java
│   │   └── AntiCheatDetector.java
│   ├── router/            # 路由分发
│   │   ├── ServiceRouter.java
│   │   ├── LoadBalancer.java
│   │   └── ServiceDiscovery.java
│   ├── crossserver/       # 跨服功能
│   │   ├── MatchService.java
│   │   ├── TeamService.java
│   │   └── RoomManager.java
│   ├── support/           # 配置
│   │   └── GatewayConfig.java
│   └── main/
│       └── GatewayStartup.java
└── pom.xml
```

## 核心功能

### 1. 连接管理
- 支持 TCP/WebSocket/HTTP 多协议
- 连接状态跟踪和管理
- 用户会话绑定

### 2. 安全防护
- **黑白名单**：IP访问控制
- **限流**：令牌桶算法，支持IP/用户/连接级别限流
- **签名校验**：HMAC-SHA256消息签名验证
- **防外挂**：速度检测、消息频率检测、数据包验证

### 3. 路由分发
- **服务发现**：自动发现和监控后端服务
- **负载均衡**：支持随机、轮询、最少连接、加权等策略
- **消息路由**：根据消息类型智能路由到对应服务

### 4. 跨服功能
- **跨服匹配**：基于等级和战力的智能匹配
- **跨服组队**：跨服务器玩家组队
- **房间管理**：跨服战斗房间管理

## 配置说明

配置类：[GatewayConfig.java](lzGatewaySrv/src/org/gof/demo/gatewaysrv/support/GatewayConfig.java)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| MAX_CONNECTIONS | 最大连接数 | 10000 |
| MAX_REQUESTS_PER_SECOND | 每秒最大请求数 | 100 |
| ENABLE_BLACK_WHITE_LIST | 启用黑白名单 | true |
| ENABLE_RATE_LIMITER | 启用限流 | true |
| ENABLE_SIGNATURE_CHECK | 启用签名验证 | true |
| ENABLE_ANTI_CHEAT | 启用防外挂 | true |
| ENABLE_CROSS_SERVER_MATCH | 启用跨服匹配 | true |
| ENABLE_CROSS_SERVER_TEAM | 启用跨服组队 | true |

---

# lzWorldSrv 架构分析

## 一、目录结构概览

```
lzWorldSrv/
├── src/org/gof/demo/
│   ├── battlesrv/         # 战斗服务模块
│   │   ├── ai/            # AI系统
│   │   ├── buff/          # Buff系统
│   │   ├── bullet/        # 子弹系统
│   │   ├── dot/           # 持续伤害系统
│   │   ├── manager/       # 战斗管理器
│   │   ├── msgHandler/    # 战斗消息处理
│   │   ├── skill/         # 技能系统
│   │   └── stageObj/      # 战斗场景对象
│   └── worldsrv/          # 世界服务模块
│       ├── activity/      # 活动系统
│       ├── character/     # 角色系统
│       ├── friend/        # 好友系统
│       ├── general/       # 武将系统
│       ├── human/         # 玩家管理
│       ├── instance/      # 副本系统
│       ├── item/          # 物品系统
│       ├── quest/         # 任务系统
│       ├── scene/         # 场景系统
│       ├── stage/         # 地图场景
│       ├── support/       # 核心支撑工具
│       └── team/          # 队伍系统
├── gen/                   # 自动生成的代码
└── target/                # 编译输出
```

## 二、核心架构模式

### 1. 分层架构

```
┌─────────────────────────────────────┐
│          客户端                      │
└───────────────┬─────────────────────┘
                │ HTTP/WebSocket
┌─────────────────────────────────────┐
│         消息路由与分发层              │
│   • 消息接收与路由                   │
│   • 协议转换                        │
└───────────────┬─────────────────────┘
                │ 内部消息
┌─────────────────────────────────────┐
│          业务管理层                  │
│   • 各种Manager（HumanManager等）   │
└───────────────┬─────────────────────┘
                │ 服务调用
┌─────────────────────────────────────┐
│          服务层                      │
│   • GameServiceBase                 │
└───────────────┬─────────────────────┘
                │ 数据操作
┌─────────────────────────────────────┐
│          数据访问层                  │
│   • 实体（Entity）                  │
└─────────────────────────────────────┘
```

### 2. 设计模式

| 模式 | 应用场景 |
|------|----------|
| **Actor模式** | 每个玩家是独立的Actor（HumanObject） |
| **事件驱动** | Event系统实现发布/订阅 |
| **观察者模式** | @Listener注解订阅事件 |
| **代理模式** | 服务调用通过Proxy代理 |
| **工厂模式** | 实体通过注解自动生成 |
| **单例模式** | ManagerBase提供单例访问 |

## 三、关键类说明

| 类 | 文件路径 | 职责 |
|---|---------|------|
| **D** | `worldsrv/support/D.java` | 分布式配置中心（服务ID、端口配置） |
| **C** | `worldsrv/support/C.java` | 系统参数配置 |
| **Event** | `worldsrv/support/Event.java` | 事件总线核心 |
| **EventKey** | `worldsrv/support/EventKey.java` | 事件类型定义枚举 |
| **ManagerBase** | 核心基类 | 所有管理器的基类 |
| **GameServiceBase** | 核心基类 | 所有服务的基类 |
| **HumanObject** | `worldsrv/human/` | 玩家核心业务对象 |
| **StageObject** | `worldsrv/stage/` | 地图场景管理对象 |

## 四、消息处理机制

### 消息流程

```
客户端请求 → Gateway → MsgHandler → Manager → Service → 数据库
                ↓
            响应消息 ← 返回结果
```

### 消息类型规范

| 前缀 | 方向 | 说明 |
|------|------|------|
| `CS*` | Client → Server | 客户端请求 |
| `SC*` | Server → Client | 服务器响应 |
| `SS*` | Server → Server | 服务间通信 |

### 消息处理器示例

```java
@MsgReceiver(CSHumanInfo.class)
public void _result_onCSHumanInfo(MsgParam param) {
    // 1. 获取玩家对象
    // 2. 处理业务逻辑
    // 3. 返回响应消息
}
```

## 五、事件驱动系统

### 事件监听示例

```java
@Listener(EventKey.HUMAN_LOGIN)
public void onHumanLogin(Param param) {
    // 处理玩家登录事件
}
```

### 核心事件类型（EventKey）

- `HUMAN_LOGIN` - 玩家登录
- `HUMAN_LOGOUT` - 玩家登出
- `STAGE_CREATE` - 场景创建
- `ITEM_GET` - 获取物品
- `QUEST_COMPLETE` - 任务完成
- 等等...

## 六、服务启动流程

```
1. 系统启动
   ├─ 加载配置（D.java、C.java）
   ├─ 初始化Event系统
   └─ 启动分布式节点

2. 服务启动
   ├─ GameServiceManager.onGameStartupBefore()
   ├─ 启动GamePort服务
   └─ 初始化下属各种服务

3. 服务初始化
   ├─ GameServiceBase.startupLocal()
   ├─ 调用init()抽象方法
   └─ 注册到Port

4. 业务模块初始化
   ├─ 各Manager通过@Listener监听启动事件
   ├─ 加载配置数据
   └─ 初始化业务状态
```

## 七、并发与线程模型

### 线程模型

- 每个 **Port** 运行在独立线程
- 每个 **Service** 有自己的消息队列
- 使用 **事件驱动** 处理并发

### 同步机制

- ManagerBase 单例模式保证线程安全
- 消息队列避免直接并发访问
- 事件系统确保线程间安全通信

### 性能优化

- **对象池**：减少GC压力
- **批量处理**：减少数据库访问
- **异步消息**：提高响应速度

## 八、架构特点

| 特点 | 说明 |
|------|------|
| ✅ **高度模块化** | 每个业务系统独立，易于维护扩展 |
| ✅ **分布式设计** | 支持多节点部署和服务发现 |
| ✅ **事件驱动** | 模块间通过事件解耦 |
| ✅ **异步处理** | 消息队列机制提高并发性能 |
| ✅ **自动生成** | 实体代码通过注解自动生成数据库访问层 |

## 九、业务模块列表

| 模块 | 说明 |
|------|------|
| activity | 活动系统 |
| battlesrv | 战斗服务（AI、Buff、技能、子弹） |
| character | 角色系统 |
| friend | 好友系统 |
| general | 武将系统 |
| human | 玩家管理 |
| instance | 副本系统 |
| item | 物品系统 |
| mail | 邮件系统 |
| quest | 任务系统 |
| rank | 排行榜 |
| scene/sceneObj | 场景系统 |
| shop | 商店系统 |
| stage | 地图场景 |
| support | 核心支撑工具 |
| team | 队伍系统 |
| tower | 塔防系统 |

---

这是一套成熟的 **分布式 MMORPG 游戏服务器** 架构，能够有效支持大量玩家的并发访问和复杂的游戏逻辑。
  
  
  
