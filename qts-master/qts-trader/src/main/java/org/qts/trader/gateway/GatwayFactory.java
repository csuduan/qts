package org.qts.trader.gateway;

import org.qts.common.disruptor.FastEventService;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.MdInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.gateway.MdGateway;
import org.qts.common.gateway.TdGateway;
import org.qts.common.utils.CommonUtil;
import org.qts.trader.gateway.ctp.CtpMdGateway;
import org.qts.trader.gateway.ctp.CtpTdGateway;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class GatwayFactory {
    static {
        try {
            long startTime=System.nanoTime();   //获取开始时间
            long endTime=System.nanoTime(); //获取结束时间
            long diff=endTime-startTime;
            System.out.println("程序运行时间： "+diff+"ns");
            String envTmpDir = "";
            String tempLibPath = "";
            try {
                if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
                    envTmpDir = System.getProperty("java.io.tmpdir");
                    tempLibPath = envTmpDir + File.separator + "mts"
                            + File.separator + "jctp" + File.separator + "lib";

                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/libiconv.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/thostmduserapi_se.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/thosttraderapi_se.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/jctpv6v3v19p1x64api.dll"));
                } else {

                    envTmpDir = "/tmp";
                    tempLibPath = envTmpDir + File.separator + "mts"
                            + File.separator + "jctp" + File.separator + "lib";

                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/libthostmduserapi_se.so"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/libthosttraderapi_se.so"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/ctp19/libjctpv6v3v19p1x64api.so"));
                }
            }catch (Exception e) {
                log.warn("复制库文件到临时目录失败", e);
            }

            if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
                System.loadLibrary("iconv");
                System.loadLibrary("thostmduserapi_se");
                System.loadLibrary("thosttraderapi_se");
                System.loadLibrary("jctpv6v3v19p1x64api");

            } else {
                System.loadLibrary("thostmduserapi_se");
                System.loadLibrary("thosttraderapi_se");
                System.loadLibrary("jctpv6v3v19p1x64api");
            }
            log.info("加载库文件成功!");
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }
    }


    public static TdGateway createTdGateway(AcctInfo account, FastEventService fastEventEngineService){
        LoginInfo loginInfo = new LoginInfo(account.getAcctConf());
        if("CTP".equals(loginInfo.getTdType()))
            return new CtpTdGateway(fastEventEngineService,loginInfo);

        throw new RuntimeException("note supported tdType,"+loginInfo.getTdType());
    }

    public static MdGateway createMdGateway(AcctInfo acctInfo, FastEventService fastEventEngineService){
        MdInfo mdInfo = new MdInfo(acctInfo.getId(),acctInfo.getAcctConf().getMdAddress());
        return new CtpMdGateway(fastEventEngineService,mdInfo);
    }
}
