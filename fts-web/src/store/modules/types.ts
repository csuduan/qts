import { RouteRecordName } from "vue-router";

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
    // 判断是否手动点击Hamburger
    isClickHamburger: boolean;
  };
  layout: string;
  device: string;
};

export type multiType = {
  path: string;
  parentPath: string;
  name: string;
  meta: any;
  query?: object;
};

export type setType = {
  title: string;
  fixedHeader: boolean;
  hiddenSideBar: boolean;
};

export type userType = {
  token: string;
  name?: string;
};

export type agentType = {
  id: string;
  name: string;
  address: string;
  status: boolean;
  enable: boolean;
};

export type AcctType = {
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
  updateTimestamp: string;
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
