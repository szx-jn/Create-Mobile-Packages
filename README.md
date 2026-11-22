<p align="center"><img src="https://github.com/user-attachments/assets/f36c5f43-2852-40fb-8535-4e7ad269eeda" alt="Logo" width="500"></p>
<h1 align="center">Create: Mobile Packages  <br>
  <a href="https://www.curseforge.com/minecraft/mc-mods/create-mobile-packages"><img src="https://cf.way2muchnoise.eu/1232978.svg" alt="CF"></a>
  <a href="https://modrinth.com/mod/create-mobile-packages"><img src="https://img.shields.io/modrinth/dt/create-mobile-packages?logo=modrinth&label=&suffix=%20&style=flat&color=242629&labelColor=5ca424&logoColor=1c1c1c" alt="Modrinth"></a>
  <a href="https://crowdin.com/project/create-mobile-packages"><img src="https://badges.crowdin.net/create-mobile-packages/localized.svg" alt="Crowdin"></a>
<br></br>
</h1>

A Minecraft mod that adds support for delivering Create Mod Packages directly to the player.

## 维度工序 NeoForge 迁移分支

本分支基于上游固定 `1.21.1-0.7.8` / `53ec8db0d247718fdadb431531426826560267cc`，移植原整合包 `1.20.1-0.6.1-dimensionworks.15` / `4e435c2a5b181e311d681a6f29d8ec0038fc24b6` 的定制功能。目标 Java 21、NeoForge 21.1.267、Create 6.0.10 官方完整 JAR、Curios 9.x 与 Mekanism 10.7.19。原注册 ID、玩家 UUID、无人机网络/货物/回收槽位与路线保留；本分支仍在整包迁移中，没有创建发行版。

保留玩家打包、五次按键脱离与免疫、包裹渲染/链式输送；实体使用原生 `ENTITY_DATA` 快照。实际 Create 开包和直接使用快照蛋恢复原 UUID，载具及乘客同时保存、移除、恢复；UUID 已占用时拒绝恢复并保留快照，避免复制或丢失。普通刷怪蛋对照路径不受影响。保存前拒绝超出 NBT 总大小或单个 modified-UTF 字符串长度的实体，拒绝时不删除原实体。旧 Forge ItemStack/EntityTag 格式需要整包的一次性离线转换，运行代码不自动猜测旧格式。

在同时安装 Sable 与 Sable In Bottle 时，移动打包器点击 Sable 物理船会借用瓶船模组的完整捕获逻辑，将整船作为一个物品放进 Create 包裹；玩家需要一个空背包位。超限时尝试用瓶船原逻辑恢复，Shift 仍可让被打包玩家主动离开 Create 链条。

Mek 纸箱使用 `mekanism:block_data` 和本模组不可变 `packager_origin` 组件；保存、掉落、重新放置后保留来源标记，工具来源拆箱不重复掉空箱。扩展打包仍执行原生领地/BreakEvent 和机器权限检查，不修改 Mek 第三方源码。无人机保留回收槽位即时保存与一次性取货；修复目标/可见实体漏更新，空载回程等待原港口，不强制加载区块，也不因港口卸载删除蜜蜂。

本地已完成构建、3 项 JUnit、19 项必需 GameTest，以及独立实际客户端的新建平坦世界检查。覆盖实体/载客身份、TNT/落沙状态、NBT 拒绝、命名箱内组件与长整数、纸箱真实掉落、普通纸箱与门/床/高花分部打包还原、无人机投递/回程/卸载区块与保存、回收槽位往返和非法数据拒绝。客户端实际通过打包载荷、第三人称包裹/蜜蜂模型加载、五次脱离、回收菜单路线/31 个命名物品同步和超大 NBT 提示。测试类、世界与日志不进入分发 JAR。这些新建夹具不等于已迁移原服务器存档；Sable/Carry On 和整包的全部 JEI/EMI/Ponder 交互还须合包验证。

验证命令：`JAVA_HOME=<Java 21> ./gradlew build test runGameTestServer`；客户端按需 `./gradlew -PcmpClientCheck runClient`，限 2 GiB / 2 CPU、30 FPS、小视距。客户端检查只使用 `run-client-check` 独立新世界。没有派发发布工作流；仅 `codex/dimensionworks-neoforge-migration` 开发分支推送不会满足现有工作流的 publish 条件。

上游依赖 Create Factory Abstractions 1.6.0 原 JAR 随上游构建配置内嵌，SHA-256 `e545dc2863842ccf473c6ad26e7ca73a37e1bb45dd529f1e5105b263320f1107`；其 JAR 许可声明 **All Rights Reserved**，不属于本项目 MIT 范围，正式整包分发前须核对其分发条件。本分支未复制或修改该依赖源码。

## Requirements

### 1.21.1 NeoForge

- NeoForge 21.1.219 or newer
- Create 6.0.10 or newer

### 1.20.1 Forge (no longer supported)
- Forge 47.3.33 or newer
- Create 6.0.8 or newer

## Download?
- Modrinth: https://modrinth.com/mod/create-mobile-packages
- Curseforge: https://www.curseforge.com/minecraft/mc-mods/create-mobile-packages

## Overview

### Logistics Network

The core of the mod is the **Logistics Network**.

- **Linking**: Bee Ports must be linked to a network. Place a new one to create a network, or link to an existing one.
- **Security**: Robo Bees only fly to ports and players within the same network.
- **Membership**: Players can join a network via the Bee Port GUI. View your networks with the `H` hotkey.

### Bee Port

The hub for your logistics network.

- **Send & Receive**: Automates package delivery to addresses (players or other ports).
- **Requirement**: Requires a **Robo Bee** item in its internal inventory to send packages.
- **Return Mode**: Toggle to have Robo Bees return to this port after delivering to a player.
- **Filter Mode**: Toggle between all items, only packages, only bees can be exported and imported by hoppers, chutes,
  funnels, etc.
- **Automation**: By default, it pulls packages from adjacent inventories. When powered by **Redstone**, it pushes items
  to adjacent inventories.

![Bee Port](https://github.com/user-attachments/assets/3b15287e-44fc-4ebc-9e59-a38fc2a5da49)

### Robo Bee

The courier entity.

- **Delivery**: Carries packages to their destination address.
- **Spawning**: Right-click with a **Robo Bee** item to spawn.
- **Network**: The bee binds to the network the item is linked to. If the item is unlinked, the bee will target any
  available port.

![Robo Bee](https://github.com/user-attachments/assets/9b78670f-a2f8-4343-bd58-5936103a9596)

### Portable Stock Ticker

A handheld device for remote item management.

- **Request Items**: Access your stock remotely by linking to Create Logistics Network.
- **Send Items**: Use the **Trash Slots** to send items from your inventory to a specific address via Robo Bee. (The Bee Port needs to be in the same network as the Portable Stock Ticker)
- **Crafting support** within the request interface
- **JEI Support**: Synchronized item search.

![Stock Ticker](https://github.com/user-attachments/assets/d8a85e58-3ffa-4c2a-8b74-48f6c2b76642)

### Mobile Packager

A utility for managing packages on the go.

- **Create**: Pack up to 9 item stacks into an addressed package.
- **Edit**: View and modify existing package contents (Sneak + Use).

![Mobile Packager](https://github.com/user-attachments/assets/9d21daf4-f64e-4df8-9ad3-a689e6f83ab5)

## Commands

Admin commands for managing logistics networks.

- `/cmp network list` - Display all networks, player counts, and owners.
- `/cmp network add <player> <networkId>` - Force add a player to a network.
- `/cmp network remove <player> <networkId>` - Force remove a player from a network.
- `/cmp robos clear` - Remove all Robo Bees from the world.

## Translations

[![Crowdin](https://badges.crowdin.net/create-mobile-packages/localized.svg)](https://crowdin.com/project/create-mobile-packages)

Help translate the mod on [Crowdin](https://crowdin.com/project/create-mobile-packages).

## Gallery

### Simple Setup
Link the Port and the Stock Link, by clicking with one item on the other block. You can link your Portable Stock Ticker by Shift-Clicking with it on the Stock Link, Port or other Linked Blocks
<img width="1920" height="1043" alt="2026-03-17_21 28 01" src="https://github.com/user-attachments/assets/9ceff20f-8445-4408-9e41-d7f28a7e6831" />

### Robo Bee on its way delivering to you
<img width="1920" height="1043" alt="2026-03-17_21 28 22" src="https://github.com/user-attachments/assets/f59466f4-b04c-48cd-9a37-b65bbeb907ee" />

### Robo Bee on its way picking up your Trash Slot items to deliver them to "Port1"
<img width="1920" height="1043" alt="2026-03-17_21 34 10" src="https://github.com/user-attachments/assets/1ef583d9-b4c8-4716-bf8e-9e120f2532e8" />

### Portable Stock Ticker selecting items to be send to you
<img width="1920" height="1043" alt="2026-03-17_21 29 00" src="https://github.com/user-attachments/assets/2abb559d-e87e-41b2-ab21-e0382ac81a77" />

### Admin Console Command to manage Networks (can also be done via UI)
<img width="1004" height="281" alt="2026-03-17_21 36 13" src="https://github.com/user-attachments/assets/b9db957e-8e66-4759-9d57-4f1c6ead4e5e" />


