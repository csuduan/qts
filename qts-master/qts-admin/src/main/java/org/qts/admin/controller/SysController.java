package org.qts.admin.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Page;
import org.qts.admin.entity.sys.Role;
import org.qts.admin.entity.sys.Router;
import org.qts.admin.entity.sys.UserInfo;
import org.qts.admin.manager.AcctManager;
import org.qts.admin.service.SysService;
import org.qts.admin.service.WebSocketService;
import org.qts.common.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Api(tags="系统管理")
@Slf4j
@RestController
@RequestMapping(value = "/v1/sys/")
public class SysController {

    @Autowired
    private SysService sysService;

    @PostMapping(path = "/user/login")
    public Response<String> login(String user, String pwd){
        Response<String> response=new Response<>();
        response.setData("admin-token");
        return response;
    }

    @PostMapping(path = "/user/logout")
    public Response<String> loginout(String user,String pwd){
        Response<String> response=new Response<>();
        response.setData("success");
        return response;
    }

    @GetMapping(path = "/user/info")
    public Response<UserInfo> getInfo(String token){
        Response<UserInfo> response=new Response<>();
        UserInfo userInfo=new UserInfo();
        userInfo.setAvatar("https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        userInfo.setName("Admin");
        userInfo.setRoles(Arrays.asList("admin"));
        response.setData(userInfo);
        return response;
    }

    @GetMapping(path = "/router")
    public Response<List<Router>> getRouters(String name){
        Response<List<Router>> response=new Response<>();
        List<Router> routers = sysService.getSysRouters(name);
        //response.setData(routers);//路由暂时由前端配置
        response.setData(new ArrayList<>());
        return response;
    }

    @GetMapping(path = "/role")
    public Response<Page<Role>> getRole(String name){
        Response<Page<Role>> response=new Response<>();
        String roleStr= """
                [
                            {
                              createTime: 1609837428000,
                              updateTime: 1645477701000,
                              creator: "admin",
                              updater: "",
                              deleted: false,
                              tenantId: 1,
                              id: 1,
                              name: "超级管理员",
                              code: "super_admin",
                              sort: 1,
                              status: 0,
                              type: 1,
                              remark: "超级管理员",
                              dataScope: 1,
                              dataScopeDeptIds: null
                            },
                            {
                              createTime: 1609837428000,
                              updateTime: 1645477700000,
                              creator: "admin",
                              updater: "",
                              deleted: false,
                              tenantId: 1,
                              id: 2,
                              name: "普通角色",
                              code: "common",
                              sort: 2,
                              status: 0,
                              type: 1,
                              remark: "普通角色",
                              dataScope: 2,
                              dataScopeDeptIds: null
                            },
                            {
                              createTime: 1609912175000,
                              updateTime: 1647698441000,
                              creator: "",
                              updater: "1",
                              deleted: false,
                              tenantId: 1,
                              id: 101,
                              name: "测试账号",
                              code: "test",
                              sort: 0,
                              status: 0,
                              type: 2,
                              remark: "132",
                              dataScope: 1,
                              dataScopeDeptIds: []
                            }
                          ]
                """;
        Page<Role> roleList=new Page<>();
        roleList.setList(JSON.parseArray(roleStr, Role.class));
        roleList.setTotal(roleList.getList().size());
        response.setData(roleList);
        return response;
    }


}
