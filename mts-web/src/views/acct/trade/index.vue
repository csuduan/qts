<script setup type="ts">
const form = {
  contract: '',
    opType: '',
    direction: '',
    price: 0,
    volume: 0
};
const tickDatas = [
  {
    contract:'ni2010',
    lastPrice:600,
    bidPrice:599,
    askPrice:601,
    time:'21:59:59'
  }
];
function onSubmit() {
  console.log('submit!');
}
</script>
<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" >
          <div slot="header" class="clearfix">
            <span>报单面板</span>
          </div>
          <el-form ref="form" :model="form" label-width="80px">
            <el-form-item label="合约">
              <el-input v-model="form.contract"></el-input>
            </el-form-item>
            <el-form-item label="开平">
              <el-radio-group v-model="form.opType">
                <el-radio label="开仓"></el-radio>
                <el-radio label="平今"></el-radio>
                <el-radio label="平仓"></el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="方向">
              <el-radio-group v-model="form.direction">
                <el-radio label="买入"></el-radio>
                <el-radio label="卖出"></el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="价格">
              <el-input v-model="form.price"></el-input>
            </el-form-item>
            <el-form-item label="手数">
              <el-input v-model="form.volume"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">报单</el-button>
              <el-button>取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card shadow="hover" >
          <div slot="header" class="clearfix">
            <span>行情</span>
          </div>
          <el-table
            :data="tickDatas"
            border
            fit
            highlight-current-row
            style="width: 100%">
            <el-table-column
              label="合约"
              prop="contract">
            </el-table-column>
            <el-table-column
              label="最新价"
              prop="lastPrice">
            </el-table-column>
            <el-table-column
              label="买一价"
              prop="bidPrice">
            </el-table-column>
            <el-table-column
              label="卖一价"
              prop="askPrice">
            </el-table-column>
            <el-table-column
              label="更新时间"
              prop="time">
            </el-table-column>
          </el-table>

        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-tabs v-model="activeName" @tab-click="handleClick">
          <el-tab-pane label="挂单" name="first">用户管理</el-tab-pane>
          <el-tab-pane label="委托单" name="second">配置管理</el-tab-pane>
          <el-tab-pane label="预埋单" name="fourth">定时任务补偿</el-tab-pane>
        </el-tabs>
      </el-col>
      <el-col :span="12">
        <el-tabs v-model="activeName" @tab-click="handleClick">
          <el-tab-pane label="持仓" name="first">用户管理</el-tab-pane>
          <el-tab-pane label="成交记录" name="second">配置管理</el-tab-pane>
        </el-tabs>
      </el-col>

    </el-row>


  </div>
</template>
