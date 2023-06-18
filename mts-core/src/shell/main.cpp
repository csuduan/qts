//
// Created by 段晴 on 2022/1/23.
//
#include "define.h"
#include <string>
#include <thread>
#include <chrono>
#include "common/socketClient.hpp"
#include "common/socketClient.hpp"
#include "filesystem"


int main(int argc,char *argv[]){
    //fmtlog::setLogFile(stdout, false);
    fmtlog::setThreadName("main");
    fmtlog::startPollingThread(1e9);

    if(!std::filesystem::exists("logs"))
        std::filesystem::create_directories("logs");

    string date=Util::getDate();
    string file="logs/shell-"+date+".log";
    fmtlog::setLogFile(file.c_str(), false);

    logi("mts-shell  start...");
    cout<<"mts-shell  start..."<<endl;
    //SocketClient* tcpClient=new SocketClient(engineId);
    //tcpClient->start();
    SocketAddr tcpAddr;
    tcpAddr.name="shell";
    tcpAddr.type=SocketType::TCP;
    tcpAddr.port=9000;
    SocketClient * client=new SocketClient(tcpAddr);
    std::thread t([&client](){
        client->start();
    });
    t.detach();

    std::thread t1([&client](){
        Event event;
        while (true){
            if(client->queue.pop(event)){
                Message *msg=(Message*)event.data;
                cout<<endl;
                cout<<msg->type<<" "<<msg->actId<<" "<<msg->data.String()<<endl;
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
                cout<<"shell> "<<std::flush;
            }else{
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            }
        }
    });
    t1.detach();

    while (true){
        cout<<"shell> "<<std::flush;
        string msg;
        //cin>>msg;
        string allMsg;
        while(getline(cin,msg))
        {
            allMsg+=msg;
            if(Util::ends_With(msg,";")){
                allMsg.pop_back();
                break;
            }
        }
        try{
            Message message;
            xpack::json::decode(allMsg,message);
            bool ret=client->request(message);
            cout<<(ret?"OK":"FAIL")<<endl;
            //cout<<"OK"<<endl;
        }catch (exception ex){
            cout<<"invalid json"<<endl;
        }
        //std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    logi("mts-shell  exit...");

}