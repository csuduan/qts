package org.qts.admin.entity;

import lombok.Data;
import org.qts.common.entity.Enums;
import org.qts.common.entity.acct.AcctInfo;

@Data
public class AcctDesc extends AcctInfo {

    private Enums.ACCT_STATUS status;
    private String updateTimes;
}
