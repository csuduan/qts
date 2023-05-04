package org.mts.admin.entity.config;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TradeConfig implements Serializable {
    private List<MdConfig> mds;
    private List<TradeEngineConfig> tradeEngines;

}
