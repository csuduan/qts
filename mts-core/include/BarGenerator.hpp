#pragma once
#include "Data.h"

class BarGenerator{
private:
    Tick * lastTick;
    Bar  * bar;
public:
    BarGenerator(){

    }
    void onTick(Tick * tick){

    }
};