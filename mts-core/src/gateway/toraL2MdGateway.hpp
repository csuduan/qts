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

};

#endif //MTS_CORE_TORAL2MDGATEWAY_H
