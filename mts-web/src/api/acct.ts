import { http } from "../utils/http";
import { ResponseType } from "./type";

// 获取账户列表
export const getAcctList = (data?: object): ResponseType => {
  return http.request("get", "/v1/acct/list", { data });
};

export const acctOperate = (data?: object): ResponseType => {
  return http.request("post", "/v1/acct/operate", { data });
};
// 获取账户明细
export const getAcctDetail = (data?: object): ResponseType => {
  return http.request("get", "/v1/acct/detail", { data });
};
