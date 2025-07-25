
<p align="center">
  <img src="./qts-logo.png" width="200">
</p>

Quick Trading Sysmtem(Qts)是一款支持多账户的量化交易系统。该系统主要由admin和trader模块组成，admin模块统一管理多个交易执行器，每个账户对应1个交易执行器(trader)，目前trader计划有2个版本：c++版本(极速版)、python版本(常规版)。

**qts当前处于开发阶段，正在快马加鞭完善中...**


## 架构

<p align="center">
  <img src="./qts-arti.png" width="400">
</p>

* qts-admin   管理平台
支持管理多个交易核心
* qts-trader  交易核心(python)
常规版本交易程序，一般延迟

* qts-web      [WEB客户端](https://github.com/csuduan/qts-web)
* qts-ctrader  [极速交易核心(C++)](https://github.com/csuduan/qts-ctrader)

## 启动
1. 安装
* 下载项目代码
* 安装python环境(略)  
* 安装依赖包
```
pip install -e .
pip install openctp-ctp==6.7.2.*
```
* 打包(可选，供生产使用)
```
pip install build
python -m build
``` 

2. 启动管理器
* 拷贝qts-simple及script到指定目录
* 启动脚本
```
cd /opt/dev/qts
python script/run_admin.py

```
浏览器访问：
docs:http://127.0.0.1:8084/docs
web:http://0.0.0.0:8084

3. 启动交易核心
```
cd /opt/dev/qts
python script/run_trader.py --acctId DQ24
```

4. 启动WEB
参见[qts-web](https://github.com/csuduan/qts-web)


## 参考
* [zeromq](https://github.com/zeromq/pyzmq)
* [openctp_python](https://github.com/openctp/openctp-ctp-python)
* [ctpbee_api](https://github.com/ctpbee/ctpbee_api/tree/master)
* [vnpy_ctp](https://github.com/vnpy/vnpy_ctp)
* [Python-CTPAPI](https://github.com/nicai0609/Python-CTPAPI)

## 问题汇总

1. 字符集问题

- Linux下安装后，需要安装中文字符集，否则导入时报错：

  ```text
  >>> import openctp_ctp
  terminate called after throwing an instance of 'std::runtime_error'
  what():  locale::facet::_S_create_c_locale name not valid
  Aborted
  ```

  或

  ```text
  >>> import openctp_ctp
  Aborted
  ```

  需要安装 `GB18030` 字符集，这里提供 ubuntu/debian/centos 的方案：

  ```bash
  # Ubuntu (20.04)
  sudo apt-get install -y locales
  sudo locale-gen zh_CN.GB18030
  
  # Debian (11)
  sudo apt install locales-all
  sudo localedef -c -f GB18030 -i zh_CN zh_CN.GB18030
  
  # CentOS (7)
  sudo yum install -y kde-l10n-Chinese
  sudo yum reinstall -y glibc-common
  ```

- Mac下报错

  ```text
  Fatal Python error: config_get_locale_encoding: failed to get the locale encoding: nl_langinfo(CODESET) failed
  Python runtime state: preinitialized
  ```

  设置 `export LANG="en_US.UTF-8"` 并使之生效
