//
// Created by 段晴 on 2022/6/7.
//

#ifndef MTS_CORE_TORAL2MDGATEWAY_H
#define MTS_CORE_TORAL2MDGATEWAY_H

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <iostream>
#include "tora/TORATstpLev2MdApi.h"
using namespace TORALEV2API;
#include "gateway.h"


class ToraL2MdGateway:public CTORATstpLev2MdSpi,public MdGateway{

public:
    ToraL2MdGateway(QuotaInfo *quotaInfo): MdGateway(quotaInfo){
    }


    void subscribe(set<string> &contracts) override{

    }
    int  connect() override{
        return 0;
    }
    void disconnect() override{

    }

};

#endif //MTS_CORE_TORAL2MDGATEWAY_H
