package org.mts.common.model.msg;

import lombok.Data;
import org.mts.common.model.acct.AcctConf;

import java.util.List;

@Data
public class ConfMsg {
    private List<AcctConf> acctConfList;
}
