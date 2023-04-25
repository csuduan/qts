/**
 * gateway接口
 */

#ifndef MTS_CORE_GATEWAY_H
#define MTS_CORE_GATEWAY_H
#include "Data.h"
#include <set>
#include <thread>
#include "fmtlog/fmtlog.h"
#include "Context.h"
class MdGateway{
protected:
    string name;
    bool connected= false;
    Quote* quote;
public:
    /// 订阅合约
    /// \param contracts
    virtual void subscribe(set<string> &contracts){};
    virtual int  connect(){return 0;};
    virtual void disconnect(){};
    MdGateway(Quote* quote):quote(quote){
        this->name=quote->name;
    }

};

class TdGateway{
protected:
    bool connected= false;
    string tradingDay;
public:
    virtual int  connect(){return 0;};
    virtual int  disconnect(){};
    virtual bool insertOrder(Order* order){};
    virtual void cancelOrder(Action* order){};
    virtual bool isConnected(){return connected;};


};

class GatewayCallback{
public:
    virtual void onTick(Tick tick){};
    virtual void onRtnTrade(){};
    virtual void onRtnOrder(){};
};



#endif //MTS_CORE_GATEWAY_H
