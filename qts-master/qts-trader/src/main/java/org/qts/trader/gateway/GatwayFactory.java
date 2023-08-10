package org.qts.trader.gateway;

import org.qts.common.disruptor.FastEventService;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.MdInfo;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.utils.CommonUtil;
import org.qts.trader.gateway.ctp.CtpMdGateway;
import org.qts.trader.gateway.ctp.CtpTdGateway;
import lombok.extern.slf4j.Slf4j;
import org.qts.trader.gateway.tora.ToraMdGateway;
import org.qts.trader.gateway.tora.ToraTdGateway;

import java.io.File;

@Slf4j
public class GatwayFactory {
    public static TdGateway createTdGateway(AcctInfo account, FastEventService fastEventService) {
        String tdType = account.getAcctConf().getTdType();
        TdGateway tdGateway = null;
        switch (tdType) {
            case "CTP" -> tdGateway = new CtpTdGateway(fastEventService, account);
            case "TORA" -> tdGateway = new ToraTdGateway(fastEventService, account);
            default -> throw new RuntimeException("note supported tdType:" + tdType);
        }
        return tdGateway;
    }

    public static MdGateway createMdGateway(AcctInfo acctInfo, FastEventService fastEventService) {
        String mdType = acctInfo.getAcctConf().getMdType();
        MdGateway mdGateway = null;
        switch (mdType) {
            case "CTP" -> mdGateway = new CtpMdGateway(fastEventService, acctInfo);
            case "TORA" -> mdGateway = new ToraMdGateway(fastEventService, acctInfo);
            default -> throw new RuntimeException("note supported mdType:" + mdType);
        }
        return mdGateway;
    }
}
