**mts 多账户交易系统**
## 模块
* mts-trade   交易程序
* mts-server  服务端(交易管理，数据管理，通讯管理)
* mts-shell   交互式命令行
## 编译
* 环境准备
``` shell
apt install cmake
apt install build-essential
apt install gdb
apt install libevent-dev
apt install libprotobuf-dev protobuf-compiler
apt install libsqlite3-dev
```

* 安装依赖包

fmtlib
```shell
git clone https://github.com/fmtlib/fmt.git
CMakeList.txt添加SET(BUILD_SHARED_LIBS ON)
cmake .
make && make install

```


## todo
* protobuf代替json

## 问题
* 权限问题
```shell
sudo chmod 755 /sys/firmware/dmi/tables/smbios_entry_point
sudo chmod 755 /dev/mem
sudo chmod 755 /sys/firmware/dmi/tables/DMI

```