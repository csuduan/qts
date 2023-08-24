package org.qts.trader.gateway.tora;

import org.qts.common.entity.Contract;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.trade.CancelOrderReq;
import org.qts.common.entity.trade.Order;
import org.qts.trader.gateway.TdGateway;

public class ToraTdGateway implements TdGateway {

    public ToraTdGateway(AcctDetail acctInfo){

    }
    @Override
    public void connect() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean insertOrder(Order orderReq) {
        return false;
    }

    @Override
    public void cancelOrder(CancelOrderReq cancelOrderReq) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public LoginInfo getLoginInfo() {
        return null;
    }

    @Override
    public Contract getContract(String symbol) {
        return null;
    }


    @Override
    public void qryContract() {

    }
}
