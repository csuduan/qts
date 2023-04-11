package com.bingbei.mts.admin.controller;

import com.bingbei.mts.admin.entity.UserInfo;
import com.bingbei.mts.common.entity.Response;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Api(tags="用户管理")
@Slf4j
@RestController
@RequestMapping(value = "/v1/user")
public class UserController {

    @PostMapping(path = "/login")
    public Response<String> login(String user, String pwd){
        Response<String> response=new Response<>();
        response.setData("admin-token");
        return response;
    }

    @PostMapping(path = "/logout")
    public Response<String> loginout(String user,String pwd){
        Response<String> response=new Response<>();
        response.setData("success");
        return response;
    }

    @GetMapping(path = "/info")
    public Response<UserInfo> getInfo(String token){
        Response<UserInfo> response=new Response<>();
        UserInfo userInfo=new UserInfo();
        userInfo.setAvatar("https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        userInfo.setName("Admin");
        userInfo.setRoles(Arrays.asList("admin"));
        response.setData(userInfo);
        return response;
    }
}
