package org.qts.trader.gateway;

import org.qts.common.entity.acct.AcctInfo;
import org.qts.trader.gateway.ctp.CtpMdGateway;
import org.qts.trader.gateway.ctp.CtpTdGateway;
import lombok.extern.slf4j.Slf4j;
import org.qts.trader.gateway.tora.ToraMdGateway;
import org.qts.trader.gateway.tora.ToraTdGateway;

@Slf4j
public class GatwayFactory {
    public static TdGateway createTdGateway(AcctInfo account) {
        String tdType = account.getAcctConf().getTdType();
        TdGateway tdGateway = null;
        switch (tdType) {
            case "CTP" -> tdGateway = new CtpTdGateway( account);
            case "TORA" -> tdGateway = new ToraTdGateway(account);
            default -> throw new RuntimeException("note supported tdType:" + tdType);
        }
        return tdGateway;
    }

    public static MdGateway createMdGateway(AcctInfo acctInfo) {
        String mdType = acctInfo.getAcctConf().getMdType();
        MdGateway mdGateway = null;
        switch (mdType) {
            case "CTP" -> mdGateway = new CtpMdGateway(acctInfo);
            case "TORA" -> mdGateway = new ToraMdGateway(acctInfo);
            default -> throw new RuntimeException("note supported mdType:" + mdType);
        }
        return mdGateway;
    }
}
