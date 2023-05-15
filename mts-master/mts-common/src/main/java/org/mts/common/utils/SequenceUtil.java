package org.mts.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SequenceUtil {
    private static UUIDKeyGenerator uuid = new UUIDKeyGenerator();
    private static int num = 1;
    private static int pid;
    private static String pidStr;
    private static String mac;

    private SequenceUtil() {
    }

    public static synchronized String getPK() {
        StringBuilder buffer = new StringBuilder(16);
        buffer.append(uuid.getCurrentTime(8));
        if (num == 9999) {
            num = 1;
        }

        String str = uuid.intToHexString(num++, 4);
        buffer.append(str);
        buffer.append(mac);
        buffer.append(pidStr);
        String seq = buffer.toString();
        if (log.isDebugEnabled()) {
            log.debug("-----------------getPK sequence（len = " + seq.length() + "）: " + seq);
        }

        return seq;
    }

    public static String getLocalSerialNo(int len) {
        if (len < 16) {
            throw new RuntimeException("无效的流水号长度:" + len);
        } else {
            return  StringUtils.leftPad(getPK(), len, "0");
        }
    }

    static {
        pid = (short)(uuid.getProcessId() & 255);
        pidStr = null;
        mac = null;
        String str = Integer.toHexString(pid).toUpperCase();
        if (str.length() < 2) {
            str = "0" + str;
        }

        pidStr = str.substring(str.length() - 2, str.length());
        mac = uuid.getMachineIDNew(2);
    }
}
