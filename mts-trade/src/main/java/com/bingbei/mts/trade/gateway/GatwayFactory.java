package com.bingbei.mts.trade.gateway;

import com.bingbei.mts.trade.gateway.ctp.CtpMdGateway;
import com.bingbei.mts.trade.gateway.ctp.CtpTdGateway;
import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.gateway.MdGateway;
import com.bingbei.mts.common.gateway.TdGateway;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@Component
@Slf4j
public class GatwayFactory {
    @PostConstruct
    public void init(){
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
    public GatwayFactory(){
    }

    public TdGateway createTdGateway(Account account, FastEventEngineService fastEventEngineService){
        if("CTP".equals(account.getLoginInfo().getTdType()))
            return new CtpTdGateway(fastEventEngineService,account.getLoginInfo());
        return null;
    }

    public MdGateway createMdGateway(MdInfo mdInfo,FastEventEngineService fastEventEngineService){
        return new CtpMdGateway(fastEventEngineService,mdInfo);
    }
}
