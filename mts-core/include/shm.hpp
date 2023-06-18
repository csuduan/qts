//
// Created by 段晴 on 2022/2/25.
//
#pragma once


#include <string>
#include <sys/shm.h>
#include <sys/ipc.h>
#include <sys/types.h>
#include <unistd.h>
#ifndef SHM_FAILED
#define SHM_FAILED -1
#endif

#include "fmtlog/fmtlog.h"
#include <filesystem>
#include <iostream>
#include <fstream>
using namespace std::filesystem;

///共享内存结构
struct MemHeader{
    long seq;//计数器，初始化0，每次更新都+1;
    long tsc;//更新时间
    int size=0;
    int lastIndex=0;//最新index
    int cachedIndex=0;//已缓存index
};


template<typename T>
class Shm{
public:
    Shm(string name, int size){
        if(!exists("/tmp/ipc/"))
            create_directories("/tmp/ipc/");
        string filename="/tmp/ipc/"+name;
        if(!exists(filename)){
            ofstream file;
            file.open(filename);
            file.close();
        }
        auto key = ftok(filename.c_str(), 1); //为共享内存生成键值
        if(key == -1)
        {
            loge("ftok fail,{}",filename);
            throw new exception;
        }
        m_key=key;
        m_size= size;
    }
    ~Shm();


    bool init(){
        //先直接attch,失败则create
        if(!this->attach()){
            if(this->create()){
                //初始化内存
                MemHeader* header=getHeader();
                header->seq=0;
                header->size=m_size;
            }else{
                return false;
            }
        }
        return true;
    }
    MemHeader * getHeader(){
        return (MemHeader*)this->m_memoryAddr;
    }
    char* getData(){
        return this->m_memoryAddr+sizeof(MemHeader);
    }
    bool attached= false;
private:
    bool create(){
        //调用创建共享内存方法，返回值初始化描述符
        int totalSize=sizeof(MemHeader) +m_size*sizeof(T);
        m_shmid = shmget(m_key, totalSize, IPC_CREAT | IPC_EXCL | SHM_R | SHM_W);
        if (m_shmid == -1)
        {
            loge("create shm fail");
            return false;
        }
        logi("create shm success,shmid:{}",m_shmid);
        //attach到共享内存，返回首地址
        m_memoryAddr = reinterpret_cast<char*>(shmat(m_shmid, NULL, 0));
        //如果返回地址为空,说明attach不成功返回false
        if (m_memoryAddr == (char*)SHM_FAILED)
        {
            loge("attach shm fail");
            return false;
        }
        this->attached=true;
        return true;
    }
    bool attach(){
        //判断相应的共享内存id是否存在
        m_shmid = shmget(m_key, 0, SHM_R|SHM_W);
        if (m_shmid == -1)
        {
            loge("get shm fail");
            return false;
        }
        logi("get shm sucess,shmid:{}",m_shmid);
        m_memoryAddr = reinterpret_cast<char*>(shmat(m_shmid, NULL, 0));	//随后调用attach方法，返回地址
        if (m_memoryAddr == (char*)SHM_FAILED){
            loge("attach shm fail,shmid:{}",m_shmid);
            return false;							//为空则返回false

        }
        logi("attach shm sucess,shmid:{}",m_shmid);
        this->attached=true;
        return true;							//成功返回true
    }
    bool detach(){
        if (m_shmid == -1 || m_memoryAddr == NULL)			//判断当前共享内存描述符以及首地址是否有效
            return true;						    //如果已经无效则直接返回true
        if (shmdt(m_memoryAddr) != 0)
        {
            if(errno == EINVAL)
            {
                m_memoryAddr = NULL;
                logw("already detached,shmid:{}",m_shmid);
                return true;
            }
            loge("shm detached fail,shmid:{}",m_shmid);
            return false;
        }
        m_memoryAddr = NULL;
        return true;
    }
    int m_key;									//共享内存key
    int m_size;									//共享内存数据区大小
    int m_shmid=-1;							    //共享内存描述符
    char *m_memoryAddr;							//共享内存地址指针
};