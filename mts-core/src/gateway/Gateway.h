/**
 * gateway接口
 */

#ifndef MTS_CORE_GATEWAY_H
#define MTS_CORE_GATEWAY_H
#include "Data.h"
#include <set>
class MdGateway{
public:
    /// 订阅合约
    /// \param contracts
    virtual void subscribe(set<string> &contracts){};
    virtual void unSubscribe(string contract){};
    virtual int connect(){return 0;};
    virtual void disconnect(){};
};

class TdGateway{
protected:
    bool connected= false;
    string tradingDay;
public:
    virtual int  connect(){return 0;};
    virtual int  disconnect(){};
    virtual void insertOrder(Order* order){};
    virtual void cancelOrder(Order* order){};
    virtual bool isConnected(){return connected;};


};

class GatewayCallback{
public:
    virtual void onTick(Tick tick){};
    virtual void onRtnTrade(){};
    virtual void onRtnOrder(){};
};



#endif //MTS_CORE_GATEWAY_H
