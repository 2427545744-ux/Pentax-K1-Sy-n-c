# Pentax K-1 Sync (K-1 一代专用现代安卓图传客户端)

专为 **PENTAX K-1（一代）** 打造的原生 Android 客户端（Kotlin + Jetpack Compose），直连 K-1 内置无线接口 `http://192.168.0.1/v1/`，彻底解决官方 Image Sync 断连、卡顿、无水印与界面陈旧的问题。

## 核心特性
1. **防断连 Wi-Fi 智能引擎**：自动搜索 `PENTAX_xxxx` 热点并调用 `ConnectivityManager.bindProcessToNetwork` 强制锁定路由，安卓系统检测到“无互联网连接”也不会切回 5G。
2. **暗房双卡槽现代图库**：独立切换 `SD1 / SD2`，支持 `全部 / JPG / RAW(PEF/DNG)` 筛选、本地缩略图磁盘缓存（二次秒开）、大图双指缩放、完整 EXIF 与实时 RGB 亮度直方图。
3. **后台保活传输队列**：支持 **预览图 (~1600px) / 原图 JPG / 无损 RAW** 三档导出，基于 Android Foreground Service 后台保活（锁屏或切去发微信不断传），内置本地去重“一键同步新照片”。
4. **专业遥控与实时取景**：Live View MJPEG 实时流、三分法/中心十字/黄金比例网格、双击 2X 检焦、参数拨盘（Av/Tv/ISO/Ev/WB）、**手动头安全快门（关闭 AF 适配老镜头）**、**间隔延时/星轨**、**B 门长曝计时器**以及 **拍完即传（Tether）**。
5. **宾得 K-1 专属水印工坊**：内置 3 款高清画框水印模板（极简白底风 / 毛玻璃圆角阴影风 / 经典双栏分割线风），支持自动读取 K-1 EXIF、手动补全老镜头型号与焦距光圈、切换宾得红/星标绿/经典黑点缀色，支持从图库直出或从手机相册选图制作。
6. **内置虚拟 K-1 演示模式 (Mock)**：安装后无需开机即可离线体验全部功能。

---

## 零环境配置：用 GitHub Actions 云端自动生成 APK

无需在电脑上安装 Android Studio，只需 3 步：

1. 在 GitHub 上新建一个仓库（公开或私有均可），例如 `PentaxK1Sync`。
2. 解压本压缩包，将目录内的所有文件（注意包含隐藏目录 `.github/workflows/build-apk.yml`）上传/推送到该仓库的 `main` 分支：
   ```bash
   git init
   git add .
   git commit -m "Initial Pentax K-1 Sync"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/PentaxK1Sync.git
   git push -u origin main
   ```
3. 打开仓库顶部的 **Actions** 标签页，等待约 2~3 分钟构建完成，点击该次运行记录，在底部 **Artifacts** 区域下载 **`PentaxK1Sync-APK`** 压缩包，解压后将 `PentaxK1Sync-v1.0.0-release.apk` 传到安卓手机即可直接安装！

---

## 连接实机说明
1. 在 PENTAX K-1 机身菜单中开启 Wi-Fi（默认 IP 为 `192.168.0.1`）。
2. 打开 **K-1 Sync** -> 切换到「连接」页 -> 确认 **虚拟 K-1 演示模式** 为关闭状态。
3. 点击 **搜索 K-1 热点**（或先在手机系统 Wi-Fi 里连上 `PENTAX_xxxx` 后点 **绑定当前 Wi-Fi**）。
4. 导出照片默认保存在系统相册 `Pictures/PentaxK1`、水印照保存在 `Pictures/PentaxK1_Watermark`、RAW（PEF/DNG）保存在 `Download/PentaxK1_RAW`。
