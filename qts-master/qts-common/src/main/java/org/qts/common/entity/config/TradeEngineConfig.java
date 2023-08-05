package org.qts.common.entity.config;

import lombok.Data;

import java.util.List;

@Data
public class TradeEngineConfig {
    private String engineId;
    private String mdId;
    private List<AcctConf> accounts;
}
