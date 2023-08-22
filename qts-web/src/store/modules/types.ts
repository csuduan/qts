import {RouteRecordName} from "vue-router";

export type cacheType = {
    mode: string;
    name?: RouteRecordName;
};

export type positionType = {
    startIndex?: number;
    length?: number;
};

export type appType = {
    sidebar: {
        opened: boolean;
        withoutAnimation: boolean;
        // 判断是否手动点击Collapse
        isClickCollapse: boolean;
    };
    layout: string;
    device: string;
};

export type multiType = {
    path: string;
    name: string;
    meta: any;
    query?: object;
    params?: object;
};

export type setType = {
    title: string;
    fixedHeader: boolean;
    hiddenSideBar: boolean;
};

export type userType = {
    username?: string;
    roles?: Array<string>;
};

export type AcctConf = {
    id: string;
    name: string;
    group: string;
    user: string;
    tdType: string;
    tdAddress: string;
    mdType: string;
    mdAddress: string;
    enable: boolean;
};

export type AcctInst = {
    id: string;
    group: string;
    user: string;
    type: string;
    enable: boolean;
    status: boolean;
    statusMsg: string;
    tdStatus: boolean;
    mdStatus: boolean;
    balance: number;
    mv: number;
    balanceProfit: number;
    closeProfit: number;
    margin: number;
    updateTimes: string;
};

export type Position = {
    id: string;
    symbol: string;
    direction: string;
    tdPos: number;
    ydpos: number;
    pos: number;
    lastPrice: number;
    lastSettle: number;
    avgPrice: number;
    holdProfit: number;
    fee: number;
    margin: number;
};

export type Trade = {
    id: string;
};

export type AcctDetail = {
    id: string;
    positions: Position[];
    trades: Trade[];
};
