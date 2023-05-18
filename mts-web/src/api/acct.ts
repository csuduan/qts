import { http } from "../utils/http";

interface ResponseType extends Promise<any> {
  data?: object;
  code?: number;
  msg?: string;
}

// 获取账户列表
export const getAcctList = (data?: object): ResponseType => {
  return http.request("get", "/v1/acct/list", { data });
};

// 获取账户明细
export const getAcctDetail = (data?: object): ResponseType => {
  return http.request("get", "/v1/acct/detail", { data });
};


