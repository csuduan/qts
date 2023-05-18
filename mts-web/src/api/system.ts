import { http } from "../utils/http";

interface ResponseType extends Promise<any> {
  data?: object;
  code?: number;
  msg?: string;
}

// 获取用户管理列表
export const getUserList = (data?: object): ResponseType => {
  return http.request("post", "/user", { data });
};

// 获取角色管理列表
export const getRoleList = (data?: object): ResponseType => {
  return http.request("get", "/v1/sys/role", { data });
};

// 获取代理列表
export const getAgentList = (data?: object): ResponseType => {
  return http.request("get", "/v1/sys/agent", { data });
};
