#pragma once
#include <stdio.h>
#include <string>

#include <thread>
#include <mutex>
#include <functional>
#include <condition_variable>

class Semaphore
{
public:
    Semaphore(int value = 1) :count(value) {}

    void wait()
    {
        std::unique_lock<std::mutex> lck(mt);
        if (--count < 0)//资源不足挂起线程
            cv.wait(lck);
    }

    void signal()
    {
        std::unique_lock<std::mutex> lck(mt);
        if (++count <= 0)//有线程挂起，唤醒一个
            cv.notify_one();
    }

private:
    int count;
    std::mutex mt;
    std::condition_variable cv;
};
