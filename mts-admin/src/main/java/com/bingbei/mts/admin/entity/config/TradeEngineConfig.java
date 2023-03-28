package com.bingbei.mts.admin.entity.config;

import lombok.Data;

import java.util.List;

@Data
public class TradeEngineConfig {
    private String engineId;
    private String mdId;
    private List<AccountConfig> accounts;
}
