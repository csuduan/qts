<script setup lang="ts">
import { useRoute } from "vue-router";
import { reactive, ref, onMounted } from "vue";
import type { TabsPaneContext } from 'element-plus'


import { useAcctStoreHook } from "@/store/modules/acct";

import Trade from "./trade.vue";
import { isString, isEmpty } from "@pureadmin/utils";

const route = useRoute();
const index = route.query?.id ?? -1;
import { useDetail } from "../hooks";


const activeName = ref('first')

const { initToDetail,toDetail } = useDetail();

const acctId = route.query.id;
if(! isEmpty(isEmpty(route.query))){
  console.info("query"+route.query)
  toDetail(route.query)
}else{
  console.error("query isEmpty")
}


const options = [
  {
    value: "ost",
    label: "ost"
  },
  {
    value: "sim",
    label: "sim"
  }
];

const handleClick = (tab: TabsPaneContext, event: Event) => {
  console.log(tab, event)
}

// 根据路由参数中的账户ID来获取特定账户的详细信息
const fetchAcctDetail = () => {
  const acctId = route.params.id; // 假设路由参数中包含id
  console.log("fetchAcctDetail,id:"+acctId)
  // const selectedAccount = useAcctStoreHook().acctDetails.find((acc) => acc.id === accountId);
  // if (selectedAccount) {
  //   account.value = { ...selectedAccount };
  // } else {
  //   // 处理账户不存在的情况
  // }
};

onMounted(() => {
  fetchAcctDetail();
});



</script>

<template>
  <div class="main">
    <el-card>
      <el-select v-model="username"  @change="onChange" class="acct-select">
        <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        />
      </el-select>

      <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
        <el-tab-pane label="交易" name="first">
          <Trade></Trade>
        </el-tab-pane>
        <el-tab-pane label="换仓" name="second">Config</el-tab-pane>
        <el-tab-pane label="策略" name="third">Role</el-tab-pane>
        <el-tab-pane label="管理" name="third">Role</el-tab-pane>
        <el-tab-pane label="日志" name="fourth">Task</el-tab-pane>
      </el-tabs>



    </el-card>

  </div>

</template>

<style lang="scss" scoped>
.demo-tabs > .el-tabs__content {
  padding: 32px;
  color: #6b778c;
  font-size: 32px;
  font-weight: 600;
}

.demo-tabs{
  margin-top: -20px;
  z-index: -30;
}

.acct-select{
}

</style>
<style lang="scss">
.demo-tabs  .el-tabs__nav {
  float: right;
}
</style>
