package com.bingbei.mts.common.exception;

import com.bingbei.mts.common.entity.Response;
import com.bingbei.mts.common.enums.ReturnCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GloablExceptionHandler {
    @ExceptionHandler({BizException.class})
    @ResponseBody
    public <T>ResponseEntity<T> handlerBizException(BizException e){
        log.error("handler bizException",e.getErrMsg());
        Response<T> response =new Response<>(e.getErrCode(), e.getErrMsg());
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public <T>ResponseEntity<T> handlerBizException(Exception e){
        log.error("handler exception",e);
        Response<T> response =new Response<>(ReturnCode.SYS_ERROR);
        return new ResponseEntity(response, HttpStatus.OK);
    }
}
