package org.mts.common.utils;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

public class UUIDKeyGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(UUIDKeyGenerator.class);
    SecureRandom secureRandom = new SecureRandom();
    private String midValue;

    public UUIDKeyGenerator() {
    }

    public String generateUUIDKey() {
        StringBuffer buffer = new StringBuffer(32);
        StringBuffer bf = new StringBuffer(16);
        byte[] addr = null;

        try {
            addr = InetAddress.getLocalHost().getAddress();
        } catch (UnknownHostException var5) {
            LOG.error("generateUUIDKey", var5);
        }

        bf.append(this.intToHexString(byteToInt(addr), 8));
        bf.append(this.intToHexString(System.identityHashCode(this), 8));
        this.midValue = bf.toString();
        buffer.append(this.intToHexString((int)(System.currentTimeMillis() & -1L), 8));
        buffer.append(this.midValue);
        buffer.append(this.intToHexString(this.secureRandom.nextInt(), 8));
        return buffer.toString();
    }

    public String getCurrentTime(int stringLength) {
        StringBuffer currentTime = new StringBuffer(stringLength);
        currentTime.append(this.intToHexString((int)(System.currentTimeMillis() / 1000L), stringLength));
        return currentTime.toString();
    }

    public String getRandomNum(int stringLength) {
        StringBuffer randomNum = new StringBuffer(stringLength);
        randomNum.append(this.intToHexString(this.secureRandom.nextInt(), stringLength));
        return randomNum.toString();
    }

    public String getMachineID(int stringLength) {
        StringBuffer machineID = new StringBuffer(stringLength);
        byte[] addr = null;

        try {
            addr = InetAddress.getLocalHost().getAddress();
        } catch (UnknownHostException var5) {
            LOG.error("getMachineID", var5);
        }

        machineID.append(this.intToHexString(byteToInt(addr), stringLength));
        return machineID.toString();
    }

    public String getMachineIDNew(int len) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            byte[] hardwareAddress = null;

            label40:
            while(interfaces.hasMoreElements()) {
                NetworkInterface inter = (NetworkInterface)interfaces.nextElement();
                hardwareAddress = inter.getHardwareAddress();
                if (hardwareAddress != null && hardwareAddress.length > 0) {
                    Enumeration inetAddressList = inter.getInetAddresses();

                    while(inetAddressList.hasMoreElements()) {
                        InetAddress inetAddress = (InetAddress)inetAddressList.nextElement();
                        String hostAddress = inetAddress.getHostAddress();
                        if (!hostAddress.equals("127.0.0.1") && !hostAddress.contains(":") && !hostAddress.contains("%")) {
                            hardwareAddress = NetworkInterface.getByInetAddress(inetAddress).getHardwareAddress();
                            if (hardwareAddress != null && hardwareAddress.length > 0) {
                                break label40;
                            }
                        }
                    }
                }
            }

            String hex = DigestUtils.md5DigestAsHex(hardwareAddress);
            return hex.substring(0, len);
        } catch (Exception var8) {
            LOG.warn("get machine id new error.", var8);
            return this.getMachineID(len);
        }
    }

    public int getProcessId() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int index = name.indexOf("@");
            return Integer.parseInt(name.substring(0, index));
        } catch (Exception var3) {
            LOG.warn("get process id error.", var3);
            return (new Random()).nextInt();
        }
    }

    public String intToHexString(int value, int stringLength) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuffer buffer = new StringBuffer(stringLength);
        int shift = stringLength - 1 << 2;
        int i = -1;

        while(true) {
            ++i;
            if (i >= stringLength) {
                return buffer.toString();
            }

            buffer.append(hexDigits[value >> shift & 15]);
            value <<= 4;
        }
    }

    public static int byteToInt(byte[] bytes) {
        int value = 0;
        int i = -1;

        while(true) {
            ++i;
            if (i >= bytes.length) {
                return value;
            }

            value <<= 8;
            int b = bytes[i] & 255;
            value |= b;
        }
    }

    public String getMidValue() {
        return this.midValue;
    }

    public void setMidValue(String midValue) {
        this.midValue = midValue;
    }
}
