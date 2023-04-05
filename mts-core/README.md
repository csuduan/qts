## 编译
* 环境安装
``` shell
apt install cmake
apt install build-essential
apt install gdb
apt install libevent-dev
apt install libprotobuf-dev protobuf-compiler
```
## 依赖
* ctp

* fmtlib
```shell
git clone https://github.com/fmtlib/fmt.git
CMakeList.txt添加SET(BUILD_SHARED_LIBS ON)
cmake .
make && make install

```

* event apt install libevent-dev
* asio


## todo
* protobuf代替json

## 问题
* 权限问题
```shell
sudo chmod 755 /sys/firmware/dmi/tables/smbios_entry_point
sudo chmod 755 /dev/mem
sudo chmod 755 /sys/firmware/dmi/tables/DMI

```