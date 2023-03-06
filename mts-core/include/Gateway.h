/**
 * gateway接口
 */

#ifndef MTS_CORE_GATEWAY_H
#define MTS_CORE_GATEWAY_H
#include "Data.h"
#include <vector>
class MdGateway{
public:
    /// 订阅合约
    /// \param contracts
    virtual void subscribe(vector<string> &contracts){};
    virtual void unSubscribe(string contract){};
    virtual int connect(){};
    virtual void disconnect(){};
};

class TdGateway{
public:
    virtual int connect(){};
    virtual void disconnect(){};
    virtual void insertOrder(Order& order){};
    virtual void cancelOrder(string orderRef){};
    virtual bool isConnect(){};

};

class GatewayCallback{
public:
    virtual void onTick(Tick tick){};
    virtual void onRtnTrade(){};
    virtual void onRtnOrder(){};
};



#endif //MTS_CORE_GATEWAY_H
