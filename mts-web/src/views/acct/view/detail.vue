<script setup lang="ts">
import { useRoute } from "vue-router";
const route = useRoute();
const index = route.query?.id ?? -1;

import { ref } from 'vue'
import type { TabsPaneContext } from 'element-plus'

const activeName = ref('first')

const handleClick = (tab: TabsPaneContext, event: Event) => {
  console.log(tab, event)
}


</script>

<template>
  <div class="main">
    <el-card>
      <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
        <el-tab-pane label="交易" name="first">User</el-tab-pane>
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

</style>
