package org.qts.common.entity.config;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TradeConfig implements Serializable {
    private List<MdConf> mds;
    private List<TradeEngineConfig> tradeEngines;

}
