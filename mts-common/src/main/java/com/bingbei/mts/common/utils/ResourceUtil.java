package com.bingbei.mts.common.utils;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceUtil {
    public static String getFileJson(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(fileName);
//        try (InputStream in=classPathResource.getInputStream()){
//            return FileCopyUtils.copyToString(new InputStreamReader(in,"UTF-8"));
//        }
        byte[]  bytes= FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
        return new String(bytes);
    }

}
