//
// Created by 段晴 on 2022/2/1.
//

#ifndef MTS_CORE_STRATEGYFACTORY_HPP
#define MTS_CORE_STRATEGYFACTORY_HPP
#include <map>
#include <string>
#include <functional>
#include <memory>
#include "strategy/strategy.h"

struct factory
{
    template<typename T>
    struct register_t
    {
        register_t(const std::string& key)
        {
            factory::get().map_.emplace(key, &register_t<T>::create);
        }

        template<typename... Args>
        register_t(const std::string& key, Args... args)
        {
            factory::get().map_.emplace(key, [=] { return new T(args...); });
        }
        inline static Strategy* create() { return new T; }
    };

    inline Strategy* produce(const std::string& key)
    {
        if (map_.find(key) == map_.end())
            throw std::invalid_argument("the message key is not exist!");

        return map_[key]();
    }

    std::unique_ptr<Strategy> produce_unique(const std::string& key)
    {
        return std::unique_ptr<Strategy>(produce(key));
    }

    std::shared_ptr<Strategy> produce_shared(const std::string& key)
    {
        return std::shared_ptr<Strategy>(produce(key));
    }
    typedef Strategy*(*FunPtr)();

    inline static factory& get()
    {
        static factory instance;
        return instance;
    }

private:
    factory() {};
    factory(const factory&) = delete;
    factory(factory&&) = delete;

    std::map<std::string, FunPtr> map_;
};

//std::map<std::string, factory::FunPtr> factory::map_;

#define REGISTER_STRATEGY_VNAME(T) reg_msg_##T##_
#define REGISTER_STRATEGY(T, key, ...) static factory::register_t<T> REGISTER_STRATEGY_VNAME(T)(key, ##__VA_ARGS__);
#endif //MTS_CORE_STRATEGYFACTORY_HPP
