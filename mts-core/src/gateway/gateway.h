/**
 * gateway接口
 */

#ifndef MTS_CORE_GATEWAY_H
#define MTS_CORE_GATEWAY_H
#include "data.h"
#include <set>
#include <thread>
#include "fmtlog/fmtlog.h"
class Acct;
class MdGateway{
protected:
    bool connected= false;
    QuoteInfo *quotaInfo;
    LockFreeQueue<Event>  *msgQueue;
    string tradingDay;

    void inline setStatus(bool  status){
        this->connected = status;
        quotaInfo->status= status;
        this->msgQueue->push(Event{EvType::STATUS,0});
    }

public:
    string id;
    MdGateway(QuoteInfo* quotaInfo): quotaInfo(quotaInfo){
        msgQueue=new LockFreeQueue<Event>(100);
        id=quotaInfo->id;
    }
    LockFreeQueue<Event> * getQueue(){
        return this->msgQueue;
    }
    /// 订阅合约
    /// \param contracts
    virtual void subscribe(set<string> &contracts) =0;
    virtual int  connect()=0;
    virtual void disconnect()=0;

};

class TdGateway{
protected:
    bool connected= false;
    LockFreeQueue<Event>  *msgQueue;
    string tradingDay;


    void inline setStatus(bool  status){
        this->connected = status;
        //quotaInfo->status= status;
        this->msgQueue->push(Event{EvType::STATUS,0});
    }
public:
    virtual int  connect(){return 0;};
    virtual int  disconnect(){};
    virtual bool insertOrder(Order* order){};
    virtual void cancelOrder(Action* order){};
    virtual bool isConnected(){return connected;};


};


#endif //MTS_CORE_GATEWAY_H
