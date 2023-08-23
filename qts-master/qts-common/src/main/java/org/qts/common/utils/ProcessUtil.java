package org.qts.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProcessUtil {
    public static int startProcess(List<String> cmds){
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmds);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            //pb.directory(new File(getWorkerDir()));
            Process process=pb.start();
            return (int)process.pid();
        }catch (Exception ex){
        }
        return 0;
    }
    public static boolean isProcessAlive(int pid){
        try {
            ProcessHandle processHandle = ProcessHandle.of(pid).orElse(null);
            if (processHandle == null) {
                return false;
            }
            return processHandle.isAlive();
        } catch (Exception e) {
            return false;
        }
    }
    public static int getProcess(String processName,String key){
        try {
            String cmd = "ps -ef |grep "+processName;
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", cmd);
            //Process process = Runtime.getRuntime().exec(cmd);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.contains(key)){
                    String pid=line.split("\\s+")[1];
                    return Integer.valueOf(pid);
                }
            }
        }catch (Exception ex){
            log.error("getProcess error",ex);
        }
        return 0;
    }
    public static boolean stopProcess(int pid){
        try {
            Process proc = Runtime.getRuntime().exec("kill -9 " + pid);
            proc.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
