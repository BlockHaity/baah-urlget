# BAAH辅助URL捕捉器

搭配[BAAH](https://github.com/sanmusen214/BAAH)使用，BA国服（也许还有其他）的大更新解决方案

注意：本软件使用AI生成

## 使用方法

1. 在Release下载最新版本安装到模拟器
2. 打开软件，设置为默认浏览器，给予存储权限。
3. 在BAAH配置中打开**大更新支持**（现在还没有）

## 开发使用指南

外部调用后，url会保存在`/storage/emulated/0/url.txt`中，使用`adb shell cat /storage/emulated/0/url.txt` 查看

获取url后，就可以下载安装。

## 构建

克隆项目后，使用android studio打开，SDK版本为API 35，使用java 11。