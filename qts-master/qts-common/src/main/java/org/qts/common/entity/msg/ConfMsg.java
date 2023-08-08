package org.qts.common.entity.msg;

import lombok.Data;
import org.qts.common.entity.config.AcctConf;

import java.util.List;

@Data
public class ConfMsg {
    private List<AcctConf> acctConfList;
}
