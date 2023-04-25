#pragma once

#include <array>
#include <vector>
#include "Data.h"

class ObjectFactory{

public:
    static ObjectFactory* get(){
        return instance;
    }

    void reset(){
        tickIndex=0;
        orderIndex=0;
        tradeIndex=0;
    }
    Tick* getTick(){
        if(tickIndex>=tickNum-1)
            return new Tick();
        else
            return &ticks[tickIndex++];
    }


private:
    static ObjectFactory *instance;
    ObjectFactory()=default;
    ObjectFactory(ObjectFactory &)=delete;
    ObjectFactory& operator=(const ObjectFactory&)=delete;


    const int tickNum=1<<20;
    int tickIndex=0;
    Tick * ticks=new Tick[tickNum];


    const int orderNum=1<<20;
    int orderIndex=0;
    Order * orders =new Order[orderNum];

    const int tradeNum=1<<10;
    int tradeIndex=0;
    Trade * trades=new Trade[tradeNum];
};

ObjectFactory* ObjectFactory::instance =new ObjectFactory();