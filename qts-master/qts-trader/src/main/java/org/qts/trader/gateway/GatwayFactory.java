package org.qts.trader.gateway;

import org.qts.common.entity.acct.AcctDetail;
import org.qts.trader.core.AcctInst;
import org.qts.trader.gateway.ctp.CtpMdGateway;
import org.qts.trader.gateway.ctp.CtpTdGateway;
import lombok.extern.slf4j.Slf4j;
import org.qts.trader.gateway.tora.ToraMdGateway;
import org.qts.trader.gateway.tora.ToraTdGateway;

@Slf4j
public class GatwayFactory {
    public static TdGateway createTdGateway(AcctInst acctInst) {
        String tdType = acctInst.getAcctDetail().getConf().getTdType();
        TdGateway tdGateway = null;
        switch (tdType) {
            case "CTP" -> tdGateway = new CtpTdGateway(acctInst);
            case "TORA" -> tdGateway = new ToraTdGateway(acctInst);
            default -> throw new RuntimeException("note supported tdType:" + tdType);
        }
        return tdGateway;
    }

    public static MdGateway createMdGateway(AcctInst acctInst) {
        String mdType = acctInst.getAcctDetail().getConf().getMdType();
        MdGateway mdGateway = null;
        switch (mdType) {
            case "CTP" -> mdGateway = new CtpMdGateway(acctInst);
            case "TORA" -> mdGateway = new ToraMdGateway(acctInst);
            default -> throw new RuntimeException("note supported mdType:" + mdType);
        }
        return mdGateway;
    }
}
