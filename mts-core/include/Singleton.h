//
// Created by 段晴 on 2022/2/25.
//

#ifndef MTS_CORE_SINGLETON_H
#define MTS_CORE_SINGLETON_H
#include <iostream>

template<typename T>
class Singleton{
public:
    static T& get() noexcept(std::is_nothrow_constructible<T>::value){
        static T instance;
        return instance;
    }
    virtual ~Singleton() noexcept{
    }
    Singleton(const Singleton&)=delete;
    Singleton& operator =(const Singleton&)=delete;
protected:
    Singleton(){
    }

};


#endif //MTS_CORE_SINGLETON_H
