## 简介
qts-trader java版本账户交易执行器。  
尽可能保持轻量，精简，以便后续迁移到C++版本。

## 运行
1. 拷贝qts.db到/opt/dev/qts
2. 设置变量
export LD_LIBRARY_PATH=/opt/dev/qts/qts-master/lib/ctp:$LD_LIBRARY_PATH
3. 运行
java -DacctId=xxx -jar qts-trader.jar