package org.mts.admin.entity;

import lombok.Data;
import org.mts.common.model.Enums;

import java.util.Map;

@Data
public class AcctOpReq {
    private Enums.MSG_TYPE type;
    private String acctId;
    private Map<String,Object> data;
}
