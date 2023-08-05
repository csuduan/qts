package org.qts.common.entity.trade;

import lombok.Data;
import org.fts.common.model.Enums;

import java.util.Map;

@Data
public class AcctOpReq {
    private Enums.MSG_TYPE type;
    private String acctId;
    private Map<String,Object> data;
}
