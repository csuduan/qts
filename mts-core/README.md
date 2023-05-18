**mts 多账户交易系统**
## 模块
* mts-trade   交易程序
* mts-server  服务端(交易管理，数据管理，通讯管理)
* mts-shell   交互式命令行
## 安装
1. 开发环境
* 安装gcc
```bash
#下载并安装 centos-release-scl
yum install --downloadonly --downloaddir=./centos-release-scl centos-release-scl
rpm -ivh centos-release-scl/*.rpm
#安装gcc开发工具
#yum install --downloadonly --downloaddir=./devtoolset devtoolset-9-toolchain
yum install --downloadonly --downloaddir=./devtoolset devtoolset-9
rpm -ivh devtoolset/*.rpm --force

#激活gcc
scl enable devtoolset-9 bash
echo "source /opt/rh/devtoolset-9/enable" >>/etc/profile

```

centos7中libstdc++版本较老，需要手动更新
```bash
cp libstdc++.so.6.0.26  /lib64/libstdc++.so.6.0.26
cd /lib64
ln -s /lib64/libstdc++.so.6.0.26 /lib64/libstdc++.so.6
```

* 安装cmake
```bash
#下载
wget https://github.com/Kitware/CMake/releases/download/v3.18.2/cmake-3.18.2.tar.gz
tar -zxvf cmake-3.18.2.tar.gz
cd cmake-3.18.2

#编译
./bootstrap
gmake && gmake install
ln -s /usr/local/bin/cmake /usr/bin/
cmake --version

```

2. 生产环境
* 安装gcc(同开发环境)


## 依赖包
* fmtlib
```bash
git clone https://github.com/fmtlib/fmt.git
CMakeList.txt添加SET(BUILD_SHARED_LIBS ON)
cmake .
make && make install

```
* libevent  
```bash
wget https://github.com/libevent/libevent/releases/download/release-2.1.12-stable/libevent-2.1.12-stable.tar.gz
./configure -prefix=/usr/local/libevent --disable-openssl 
make & sudo make install
或者
yum install libevent
```

* sqlite3
```bash
yum install sqlite-devel
sqlite3 -version


```

* protobuf

## 问题
* 权限问题
```shell
sudo chmod 755 /sys/firmware/dmi/tables/smbios_entry_point
sudo chmod 755 /dev/mem
sudo chmod 755 /sys/firmware/dmi/tables/DMI

```