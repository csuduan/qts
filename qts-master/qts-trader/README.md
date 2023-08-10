# 运行
1. 拷贝qts.db到/opt/dev/qts
2. 设置变量
export LD_LIBRARY_PATH=/opt/dev/qts/qts-master/lib/ctp:$LD_LIBRARY_PATH
3. 运行
java -DacctId=xxx -jar qts-trader.jar