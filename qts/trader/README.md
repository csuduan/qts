## 概述

## 环境准备
1. 安装python及依赖包
2. 安装api(当前使用openapi)
在api目录(默认/opt/data/api)安装api包,可以在config.py中设置
```zsh
#开发环境为了能有代码提示
pip install openctp-ctp==6.6.7.\* -i https://pypi.tuna.tsinghua.edu.cn/simple --trusted-host=pypi.tuna.tsinghua.edu.cn
```

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