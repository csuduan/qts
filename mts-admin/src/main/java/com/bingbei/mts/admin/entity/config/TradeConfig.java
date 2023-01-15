package com.bingbei.mts.admin.entity.config;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TradeConfig implements Serializable {
    private List<AccountConfig> accountConfigs;
    private List<MdConfig> mdConfigs;
    private List<TradeEngineConfig> tradeEngineConfigs;

}
