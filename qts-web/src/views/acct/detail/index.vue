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
      <div class="acct-desc">
        <span>{{acctId}}</span>
        <el-tag
            :type="'danger'"
            disable-transitions
        >
          交易
        </el-tag>
        <el-divider direction="vertical" class="split-desc"/>
        <el-tag
            :type="'danger'"
            disable-transitions
        >
          行情
        </el-tag>
        <span>交易日：{{20230822}}</span>
        <span>市值：{{'--'}}</span>
        <span>保证金：{{'--'}}</span>
        <span>持仓盈亏：{{'--'}}</span>
        <span>手续费：{{'--'}}</span>

      </div>

      <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
        <el-tab-pane label="交易" name="first">
          <Trade></Trade>
        </el-tab-pane>
        <el-tab-pane label="换仓" name="second">Config</el-tab-pane>
        <el-tab-pane label="策略" name="third">Role</el-tab-pane>
        <el-tab-pane label="管理" name="fourth">Role</el-tab-pane>
        <el-tab-pane label="日志" name="five">Task</el-tab-pane>
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
  margin-top: -30px;
  //z-index: -30;
}

.acct-desc > span{
  margin: 0 10px;
}

.split-desc{
  margin: 0 -5px;
}

.acct-select{
}


</style>
<style lang="scss">
.demo-tabs  .el-tabs__nav {
  float: right;
}
</style>
