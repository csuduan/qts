
<p align="center">
  <img src="./qts-logo.png" width="200">
</p>

Quick Trading Sysmtem(Qts)是一款支持多账户的量化交易系统。该系统由1个master+n个trader模块组成，master模块同意管理多个交易执行器，每个账户对应1个交易执行器(trader)，支持java(通用版)、c++(极速版)、python版本(快捷版)。

**qts当前处于开发阶段，正在快马加鞭完善中...**


## 架构

<p align="center">
  <img src="./qts-arti.png" width="400">
</p>


* qts-ctrader   交易核心(C++)
C++版交易程序，低延迟(tick2trade<5us)
* qts-trader    交易核心
常规版本交易程序，一般延迟
* qts-master   管理中心
支持管理多个交易核心
* qts-web     WEB客户端



## 部署启动
1. 环境准备
* 服务器安装jdk17
* 拷贝qts.sqlite到服务器中/opt/dev/qts
* 拷贝接口依赖库lib到/opt/dev/qts
* 拷贝jar依赖包jars到/opt/dev/qts
* 设置环境变量  
```
export LD_LIBRARY_PATH=/opt/dev/qts/lib/ctp:$LD_LIBRARY_PATH
```

2. 启动管理器
```
cd /opt/dev/qts
java -jar qts-admin.jar
```

3. 启动交易核心(手动)
```
cd /opt/dev/qts
java -DacctId=xxx -jar qts-trader.jar  
```

4. 启动WEB
* 执行命令 npm run serve
* 浏览器访问http://localhost:8080/



## 参考
* CTP封装
  https://github.com/sun0x00/swig-java-ctp
```shell
#生成代码
mkdir java_src
swig  -c++ -java -package xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api -outdir java_src -o jctpv6v3v19p1x64api_wrap.cpp jctpv6v3v19p1x64api.i
#编译及打包
javac java_src/*.java
mkdir  -p xyz/redtorch/gateway/ctp/x64v6v3v19p1v/api   
cp java_src/*.class  -p xyz/redtorch/gateway/ctp/x64v6v3v19p1v/api  
jar cf jctp-6.3.19.jar xyz                 

 ```
* redtorch
  https://github.com/sun0x00/redtorch
* 支持的网关类型
  CTP
  OST
  TORA