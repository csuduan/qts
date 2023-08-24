import { defineStore } from "pinia";
import { store } from "@/store";
import { AcctConf, AcctInst, Position } from "@/store/modules/types";

export const useAcctStore = defineStore({
    id: "websocket",
    state: () => ({
        acctInited: false,
        acctInfos: [],
        acctConfs: [],
        acctDetails: {}
    }),
    actions: {
        setAcctConfis(acctConfs:AcctConf[]){
            this.acctConfs = acctConfs
        },
        setAcctInfos(acctInfos:AcctInst[]){
            this.acctInfos = acctInfos
        },
        updateAcctInfo(newAcctInst:AcctInst) {
            const index = this.acctInfos.findIndex((acc) => acc.id === newAcctInst.id);
            if (index !== -1) {
                const selected = this.acctInfos[index].isSelected;
                this.acctInfos[index] = { ...this.acctInfos[index], ...newAcctInst };
                this.acctInfos[index].isSelected = selected;//保持选中状态不变
            }
        },
        updateAcctConf(newAcctConf:AcctConf) {
            const index = this.acctConfs.findIndex((acc) => acc.id === newAcctConf.id);
            if (index !== -1) {
                this.acctConfs[index] = { ...this.acctConfs[index], ...newAcctConf };
            }
        }

    }
});

export function useAcctStoreHook() {
    return useAcctStore(store);
}
