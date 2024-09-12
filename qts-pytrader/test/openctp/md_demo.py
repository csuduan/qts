# -*- coding: utf-8 -*-

from gateway.api_loader import load_library
#
#from openctp_ctp import mdapi

ctp = load_library('ctp',None)
mdapi = ctp.mdapi


class CMdImpl(mdapi.CThostFtdcMdSpi):
    def __init__(self, md_front):
        mdapi.CThostFtdcMdSpi.__init__(self)
        self.md_front = md_front
        self.api = None

    def Run(self,flow_path):
        self.api = mdapi.CThostFtdcMdApi.CreateFtdcMdApi(flow_path)
        mdapi.CThostFtdcMdApi.GetApiVersion()
        self.api.RegisterFront(self.md_front)
        self.api.RegisterSpi(self)
        self.api.Init()

    def OnFrontConnected(self) -> "void":
        print("OnFrontConnected")

        # Market channel doesn't check userid and password.
        req = mdapi.CThostFtdcReqUserLoginField()
        self.api.ReqUserLogin(req, 0)

    def OnFrontDisconnected(self, nReason: int) -> "void":
        print(f"OnFrontDisconnected.[nReason={nReason}]")

    def OnRspUserLogin(self, pRspUserLogin: 'CThostFtdcRspUserLoginField', pRspInfo: 'CThostFtdcRspInfoField', nRequestID: 'int', bIsLast: 'bool') -> "void":
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            print(f"Login failed. {pRspInfo.ErrorMsg}")
            return
        print(f"Login succeed.{pRspUserLogin.TradingDay}")

        self.api.SubscribeMarketData(["au2406".encode('utf-8')], 1)

    def OnRtnDepthMarketData(self, pDepthMarketData: 'CThostFtdcDepthMarketDataField') -> "void":
        print(f"{pDepthMarketData.InstrumentID} - {pDepthMarketData.LastPrice} - {pDepthMarketData.Volume}")

    def OnRspSubMarketData(self, pSpecificInstrument: 'CThostFtdcSpecificInstrumentField', pRspInfo: 'CThostFtdcRspInfoField', nRequestID: 'int', bIsLast: 'bool') -> "void":
        if pRspInfo is not None and pRspInfo.ErrorID != 0:
            print(f"Subscribe failed. [{pSpecificInstrument.InstrumentID}] {pRspInfo.ErrorMsg}")
            return
        print(f"Subscribe succeed.{pSpecificInstrument.InstrumentID}")


if __name__ == '__main__':
    md = CMdImpl("tcp://180.168.146.187:10131")
    md.Run('/opt/data/ctp_con/demo_md')
    input("Press enter key to exit.")