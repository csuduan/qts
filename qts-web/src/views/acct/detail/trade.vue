<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <span>行情</span>
        <el-table
          :data="tickDatas"
          :size="'small'"
          :height="250"
          border
          fit
          highlight-current-row
        >
          <el-table-column label="合约" width="60" prop="contract" />
          <el-table-column label="买一" prop="bidPrice" />
          <el-table-column label="卖一" prop="askPrice" />
          <el-table-column label="时间" prop="time" />
        </el-table>
        <el-divider />
        <el-form ref="form" :size="'small'" :model="form" label-width="80px">
          <el-form-item label="合约">
            <el-input v-model="form.contract" />
          </el-form-item>
          <el-form-item label="开平">
            <el-radio-group v-model="form.opType">
              <el-radio label="开仓" />
              <el-radio label="平今" />
              <el-radio label="平仓" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="方向">
            <el-radio-group v-model="form.direction">
              <el-radio label="买入" />
              <el-radio label="卖出" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="价格">
            <el-input v-model="form.price" />
          </el-form-item>
          <el-form-item label="手数">
            <el-input v-model="form.volume" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSubmit">报单</el-button>
            <el-button>取消</el-button>
          </el-form-item>
        </el-form>
      </el-col>

      <el-col :span="18">
        <span>持仓</span>
        <el-table
          :data="tickDatas"
          :size="'small'"
          :height="250"
          border
          fit
          highlight-current-row
        >
          <el-table-column label="合约" prop="contract" />
          <el-table-column label="方向" prop="lastPrice" />
          <el-table-column label="今仓" prop="bidPrice" />
          <el-table-column label="昨仓" prop="askPrice" />
          <el-table-column label="总仓" prop="time" />
          <el-table-column label="最新价" prop="time" />
          <el-table-column label="昨结" prop="time" />
          <el-table-column label="持仓均价" prop="time" />
          <el-table-column label="持仓盈亏" prop="time" />
          <el-table-column label="手续费" prop="time" />
          <el-table-column label="保证金" prop="time" />
        </el-table>
        <ElDivider />
        <el-tabs v-model="activeName" class="demo-tabs">
          <el-tab-pane label="挂单" name="first">
            <el-table
              :data="tickDatas"
              :size="'small'"
              border
              fit
              highlight-current-row
            >
              <el-table-column label="报单编号" prop="contract" />
              <el-table-column label="系统编号" prop="lastPrice" />
              <el-table-column label="交易日期" prop="bidPrice" />
              <el-table-column label="时间" prop="askPrice" />
              <el-table-column label="合约" prop="time" />
              <el-table-column label="方向" prop="time" />
              <el-table-column label="开平" prop="time" />
              <el-table-column label="报单手数" prop="time" />
              <el-table-column label="状态" prop="time" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="成交" name="fourth">
            <el-table
              :data="tickDatas"
              :size="'small'"
              border
              fit
              highlight-current-row
            >
              <el-table-column label="成交标号" prop="contract" />
              <el-table-column label="交易日" prop="lastPrice" />
              <el-table-column label="时间" prop="bidPrice" />
              <el-table-column label="合约" prop="askPrice" />
              <el-table-column label="方向" prop="time" />
              <el-table-column label="开平" prop="time" />
              <el-table-column label="手数" prop="time" />
              <el-table-column label="成交价" prop="time" />
              <el-table-column label="手续费" prop="time" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";

const activeName = ref('first')

const form = {
  contract: "",
  opType: "",
  direction: "",
  price: 0,
  volume: 0
};
const tickDatas = [
  {
    contract: "ni2010",
    lastPrice: 600,
    bidPrice: 599,
    askPrice: 601,
    time: "21:59:59"
  }
];
function onSubmit() {
  console.log("submit!");
}
</script>

<style scoped></style>
