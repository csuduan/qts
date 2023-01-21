package com.bingbei.mts.adapter;

import com.bingbei.mts.adapter.ctp.CtpTdGateway;
import com.bingbei.mts.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class LibLoader {
    static {
        try {
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
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }

    }
}
