//
// Created by 段晴 on 2022/1/24.
//
#include <iostream>
#include <fstream>
#include "Configure.h"
#include "json/json.h"
#include "Data.h"

void Configure::load(string engineId,std::string file) {
    try {
        Json::Reader reader ;
        Json::Value root ;
        ifstream in(file.data(),ios::binary);
        if(!in.is_open()){
            cout << "can not open file " << file << endl ;
            exit(1);
        }
        reader.parse(in,root);
        in.close();

        Json::Value tradeEngines=root["tradeEngines"];
        for(int i=0;i<tradeEngines.size();i++){
            Json::Value tradeEngine=tradeEngines[i];
            if(engineId!=tradeEngine["engineId"].asString())
                continue;
            string mdId=tradeEngine["mdId"].asString();
            Json::Value accounts=tradeEngine["tradeEngine"];
            for(int j=0;j<accounts.size();j++){
                auto accountNode=accounts[i];
                string id=accountNode["id"].asString();
                string name=accountNode["name"].asString();
                string user=accountNode["user"].asString();
                string passwd=accountNode["user"].asString();
                string address=accountNode["tdAddress"].asString();
                Account account;
                account.id=id;
                account.name=name;
                account.loginInfo.accoutId=id;
            }
        }
    }catch (exception ex){
        cout<< "load config file error"<<endl;
    }
}
