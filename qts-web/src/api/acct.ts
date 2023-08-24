import { http } from "@/utils/http";
import { ResponseType } from "./type";
import {AcctInst} from "@/store/modules/types";

/**
 * 账户配置相关接口
 * @param data
 */
export const getAcctConfList = (data?: object): ResponseType => {
    return http.request("get", "/api/v1/acct/conf/list", { data });
};
export const saveAcctConf = (data?: object): ResponseType => {
    return http.request("post", "/api/v1/acct/conf", { data });
};
export const delAcctConf = (data?: object): ResponseType => {
    return http.request("delete", "/api/v1/acct/conf", { data });
};

/**
 * 账户实例相关接口
 */
export const getAcctInstList = (data?: object): ResponseType<AcctInst[]> => {
    return http.request("get", "/api/v1/acct/inst/list", { data });
};
export const getAcctInstDetail = (data?: object): ResponseType => {
    return http.request("get", "/api/v1/acct/inst/detail", { data });
};
export const startAcctInst = (data?: object): ResponseType => {
    return http.request("get", "/api/v1/acct/inst/start", { data });
};
export const stopAcctInst = (data?: object): ResponseType => {
    console.info("stop inst ",data)
    return http.get("/api/v1/acct/inst/stop", {params:data});
};

/**
 * 交易相关接口
 */
export const acctOperate = (data?: object): ResponseType => {
    return http.request("post", "/api/v1/acct/trade/operate", { data });
};
