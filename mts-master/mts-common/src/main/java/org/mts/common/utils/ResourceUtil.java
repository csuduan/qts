package org.mts.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

public class ResourceUtil {
    public static String getFileJson(String fileName)  {
        try {
            ClassPathResource classPathResource = new ClassPathResource(fileName);
//        try (InputStream in=classPathResource.getInputStream()){
//            return FileCopyUtils.copyToString(new InputStreamReader(in,"UTF-8"));
//        }
            byte[]  bytes= FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            return new String(bytes);
        }catch (Exception ex){
            throw  new RuntimeException("读文件失败");
        }

    }

}
