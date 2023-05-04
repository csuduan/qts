package org.mts.admin.utils;

import org.mts.admin.exception.BizException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

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
            throw  new BizException("读文件失败");
        }

    }

}
