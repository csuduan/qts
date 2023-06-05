package org.mts.common.model.msg;

import lombok.Data;
import org.mts.common.model.conf.AcctConf;

import java.util.List;

@Data
public class ConfMsg {
    private List<AcctConf> acctConfList;
}
