# 媒体存储设备管理

防止媒体存储滥用的 Xposed 模块。

[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/MediaProviderManager)
[![Stars](https://img.shields.io/github/stars/MaterialCleaner/Media-Provider-Manager?label=Stars)](https://github.com/MaterialCleaner/Media-Provider-Manager)
[![Download](https://img.shields.io/github/v/release/MaterialCleaner/Media-Provider-Manager?label=Download)](https://github.com/MaterialCleaner/Media-Provider-Manager/releases/latest)

## 截屏

<p><img src="https://raw.githubusercontent.com/MaterialCleaner/Media-Provider-Manager/main/screenshots/about.jpg" height="400" alt="Screenshot"/>
<img src="https://raw.githubusercontent.com/MaterialCleaner/Media-Provider-Manager/main/screenshots/record.jpg" height="400" alt="Screenshot"/>
<img src="https://raw.githubusercontent.com/MaterialCleaner/Media-Provider-Manager/main/screenshots/template.jpg" height="400" alt="Screenshot"/></p>

## 什么是媒体存储

[媒体存储库][1]是安卓系统提供的媒体文件索引。当应用需要访问媒体文件时（如相册应用想显示设备中的全部图片），相比于遍历外部存储卷中的全部文件，[使用媒体存储][2]更加高效、方便。另外它还能减少应用可访问的文件，有利于保护用户隐私。

## 媒体存储如何被滥用

与原生存储空间一样，安卓系统没有提供媒体存储的精细管理方案。
- 应用只需要低危权限就可以访问全部媒体文件，用户无法限制读取范围
- 无需权限即可写入文件，应用随意写入文件会让存储空间和媒体库混乱，而且还能借此实现跨应用追踪

## 特性

- 媒体文件管理器（无需 root 或 Xposed，仅需要[读取外部存储空间][3]权限）
- 过滤媒体存储返回的数据，保护隐私数据不被查询
- 防止应用通过媒体存储随意写入文件
- 历史记录功能，了解应用是否使用媒体存储，是否有滥用情况
- 阻止 💩 ROM 的下载管理程序创建不规范文件
- 质感设计 3 界面，简单易用
- 开源

## 源代码

[https://github.com/MaterialCleaner/Media-Provider-Manager](https://github.com/MaterialCleaner/Media-Provider-Manager)

## 发行版

[Github Release](https://github.com/MaterialCleaner/Media-Provider-Manager/releases/latest)

## 协议

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

[1]: https://developer.android.com/reference/android/provider/MediaStore
[2]: https://developer.android.com/training/data-storage/use-cases?hl=zh-cn#handle-media-files
[3]: https://developer.android.com/reference/android/Manifest.permission.html#READ_EXTERNAL_STORAGE
