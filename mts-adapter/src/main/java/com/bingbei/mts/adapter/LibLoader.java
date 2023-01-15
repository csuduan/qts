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

                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/win/libiconv.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/win/ctp/thostmduserapi.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/win/ctp/jctpmdapiv6v3v11x64.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/win/ctp/thosttraderapi.dll"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/win/ctp/jctptraderapiv6v3v11x64.dll"));
                } else {

                    envTmpDir = "/tmp";
                    tempLibPath = envTmpDir + File.separator + "mts"
                            + File.separator + "jctp" + File.separator + "lib";

                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/linux/ctp/libthostmduserapi.so"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/linux/ctp/libthosttraderapi.so"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/linux/ctp/libjctpmdapiv6v3v11x64.so"));
                    CommonUtil.copyURLToFileForTmp(tempLibPath, CtpTdGateway.class.getResource("/lib/linux/ctp/libjctptraderapiv6v3v11x64.so"));
                }
            }catch (Exception e) {
                log.warn("复制库文件到临时目录失败", e);
            }

            if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
                System.loadLibrary("iconv");
                System.loadLibrary("thostmduserapi");
                System.loadLibrary("jctpmdapiv6v3v11x64");
                System.loadLibrary("thosttraderapi");
                System.loadLibrary("jctptraderapiv6v3v11x64");

            } else {
                System.loadLibrary("thostmduserapi");
                System.loadLibrary("jctpmdapiv6v3v11x64");
                System.loadLibrary("thosttraderapi");
                System.loadLibrary("jctptraderapiv6v3v11x64");
            }
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }

    }
}
