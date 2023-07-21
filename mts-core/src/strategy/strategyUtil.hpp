//
// Created by 段晴 on 2022/6/15.
//

#ifndef MTS_CORE_STRATEGYUTIL_H
#define MTS_CORE_STRATEGYUTIL_H

#include <string>
#include "config.h"
#include "xpack/thirdparty/rapidxml/rapidxml.hpp"
#include "xpack/thirdparty/rapidxml/rapidxml_utils.hpp"
#include "common/util.hpp"

using namespace rapidxml;

class StrategyUtil {
public:
    static vector<StrategySetting> parseConfig(string path) {
        vector<StrategySetting> list;

        rapidxml::file<> fdoc(path.c_str());
        rapidxml::xml_document<> doc;
        doc.parse<0>(fdoc.data());
        rapidxml::xml_node<> *root = doc.first_node();
        //遍历<strategy>
        for (rapidxml::xml_node<> *node = root->first_node();
             node; node = node->next_sibling()) {
            std::map<string, string> attrs = getAttrs(node);

            StrategySetting setting{};
            setting.strategyId = attrs["id"];
            setting.refSymbol = attrs["refid"];
            setting.trgSymbol = attrs["trgid"];
            setting.strategyType = attrs["type"];
            setting.barLevel = BAR_LEVEL::T1;
            setting.paramMap = std::move(attrs);
            list.emplace_back(setting);
        }
        return std::move(list);
    }

private:
    static std::map<string, string> getAttrs(rapidxml::xml_node<> *node) {
        std::map<string, string> attrs;
        if (node != nullptr) {
            for (xml_attribute<> *attr = node->first_attribute();
                 attr; attr = attr->next_attribute()) {
                attrs[attr->name()] = attr->value();
            }
        }
        return attrs;
    }
};

#endif //MTS_CORE_STRATEGYUTIL_H
