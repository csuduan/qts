import { defineStore } from "pinia";
import { store } from "/@/store";
import { AcctDetail, AcctType, Position } from "/@/store/modules/types";

export const useAcctStore = defineStore({
  id: "websocket",
  state: () => ({
    acctInited: false,
    acctList: [],
    acctDetails: {}
  }),
  getters: {
    getAcctList() {
      return this.acctList;
    }
  },
  actions: {
    init(agents: AcctType[]) {
      this.acctList = [];
      agents.forEach(x => this.acctList.push(x));
      this.acctInited = true;
    },
    getAcctDetail(id: string) {
      return this.acctDetails[id];
    },
    saveAcct(acct: AcctType) {
      if (!this.acctInited) return;
      let find = false;
      this.acctList.forEach((x: AcctType) => {
        if (x.id == acct.id) {
          find = true;
          for (const i in acct) {
            x[i] = acct[i];
          }
          console.log(x);
          console.log(acct);
        }
      });
      if (find == false) {
        this.acctList.push(acct);
      }
    },
    saveAcctPosition(id: string, position: Position) {
      const acctDetail: AcctDetail = this.getAcctDetail(id);
      const positions = acctDetail.positions;
      let find = false;
      this.positions.forEach((x: Position) => {
        if (x.id == position.id) {
          find = true;
          //更新
          for (const i in position) {
            x[i] = position[i];
          }
        }
      });
      if (find == false) {
        this.positions.push(position);
      }
    }
  }
});

export function useAcctStoreHook() {
  return useAcctStore(store);
}
