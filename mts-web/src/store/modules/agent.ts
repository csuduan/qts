import { defineStore } from "pinia";
import { store } from "/@/store";
import { agentType } from "/@/store/modules/types";

export const useAgentStore = defineStore({
  id: "websocket",
  state: () => ({
    agentInited: false,
    agentList: []
  }),
  getters: {
    getAgentList() {
      return this.agentList;
    }
  },
  actions: {
    init(agents: agentType[]) {
      this.agentList = [];
      agents.forEach(x => this.agentList.push(x));
      this.agentInited = true;
    },
    updateAgent(agent: agentType) {
      if (!this.agentInited) return;
      let find = false;
      this.agentList.forEach((x: agentType) => {
        if (x.id == agent.id) {
          find = true;
          //更新
          x.name = agent.name;
          x.address = agent.address;
          x.status = agent.status;
          x.enable = agent.enable;
        }
      });
      if (find == false) {
        this.agentList.push(agent);
      }
    }
  }
});

export function useAgentStoreHook() {
  return useAgentStore(store);
}
