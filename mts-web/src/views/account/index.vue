<template>
  <div class="dashboard-container">
    <el-row >
      <el-select v-model="accountValue" @change="accountChanged" placeholder="选择账户" size="small">
        <el-option
          v-for="item in accounToptions"
          :key="item.value"
          :label="item.label"
          :value="item.value">
        </el-option>
      </el-select>

    </el-row>
    <br/>


    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>账户持仓</span>
      </div>
      <el-form label-position="left" inline class="demo-table-expand">
        <el-form-item label="静态市值">
          <span>1000</span>
        </el-form-item>
        <el-form-item label="可用市值">
          <span>1000</span>
        </el-form-item>
        <el-form-item label="持仓盈亏">
          <span>1000</span>
        </el-form-item>
        <el-form-item label="已实现盈亏">
          <span>1000</span>
        </el-form-item>
        <el-form-item label="手续费">
          <span>1000</span>
        </el-form-item>
      </el-form>

      <br/>
      <el-table
        :data="positions"
        border
        fit
        highlight-current-row
        style="width: 100%">
        <el-table-column
          label="合约"
          prop="contract">
        </el-table-column>
        <el-table-column
          label="方向"
          prop="direction">
        </el-table-column>
        <el-table-column
          label="今仓"
          prop="td">
        </el-table-column>
        <el-table-column
          label="昨仓"
          prop="yd">
        </el-table-column>
        <el-table-column
          label="总仓"
          prop="total">
        </el-table-column>
        <el-table-column
          label="持有盈亏"
          prop="profit">
        </el-table-column>
      </el-table>

    </el-card>
    <el-card class="box-card" style="margin-top: 40px">
      <div slot="header" class="clearfix">
        <span>账户策略</span>
      </div>
      <el-table
      :data="tableData2"
      :span-method="arraySpanMethod2"
      border
      style="width: 100%">
      <el-table-column
        prop="id"
        label="策略ID"
        width="180">
      </el-table-column>
      <el-table-column
        prop="level"
        label="级别">
      </el-table-column>


      <el-table-column
        prop="contract"
        label="合约">
      </el-table-column>
      <el-table-column
        prop="time"
        label="时间">
      </el-table-column>
      <el-table-column
        prop="price"
        label="价格">
      </el-table-column>
      <el-table-column
        prop="volume"
        label="手数">
      </el-table-column>
      <el-table-column
        prop="long"
        label="多单">
      </el-table-column>
      <el-table-column
        prop="short"
        label="空单">
      </el-table-column>
      <el-table-column
        prop="profit"
        label="盈亏">
      </el-table-column>
      <el-table-column
        prop="pause"
        label="暂停">
        <el-checkbox >停开</el-checkbox>
        <el-checkbox >停平</el-checkbox>
      </el-table-column>
    </el-table>
    </el-card>
    <el-card class="box-card" style="margin-top: 40px">
      <div slot="header" class="clearfix">
        <span>账户日志</span>
      </div>
      <div>https://www.cnblogs.com/April-Chou-HelloWorld/p/10187690.html</div>
      <div>https://www.cnblogs.com/shihaiming/p/6201678.html</div>


    </el-card>

  </div>
</template>

<script>
  import { mapGetters } from 'vuex'

  export default {
    name: 'Dashboard',
    data() {
      return {
        accounToptions: [{
          value: '上期仿真-DQ',
          label: '上期仿真-DQ'
        }, {
          value: '上期24-DQ',
          label: '上期24-DQ'
        },{
          value: '华泰主席-ZLZ',
          label: '华泰主席-ZLZ'
        }],
        accountValue: '',
        positions:[
          {
            contract:'ni2010',
            direction:'Long',
            td:5,
            yd:5,
            total:10,
            profit:500
          },
          {
            contract:'ni2010',
            direction:'Short',
            td:5,
            yd:5,
            total:10,
            profit:500
          }
        ],
        tableData2: []
      }
    },
    computed: {
      ...mapGetters([
        'name'
      ])
    },
    created() {
      this.getTable()
    },
    methods: {
      accountChanged(row) {
        console.log('account:'+row);
      },
      getTable() {
        const tableData = [{
          id: 'ColorArbi-NI1',
          level: 'M1',
          profit:0,
          contracts: [{
            contract: 'ni1901',
            volume:10,
            long:0,
            short:0
          }, {
            contract: 'ni1905',
            volume:10,
            long:0,
            short:0
          }]
        }, {
          id: 'ColorArbi-ZN1',
          level: 'M1',
          profit:0,
          contracts: [{
            contract: 'zn1901',
            volume:10,
            long:0,
            short:0
          }, {
            contract: 'zn1905',
            volume:10,
            long:0,
            short:0
          },{
            contract: 'zn1909',
            volume:10,
            long:0,
            short:0
          }]
        }]
        this.dealTable(tableData)
      },

      // 处理表格数据(level,表格合并的层级)
      dealTable(tableData, level = 2) {
        const getDate = [] // 存储新表格数据
        let a // id,地区需要合并的行是所有类型的长度
        tableData.forEach((v, index) => {
          if (v.contracts && v.contracts.length) {
            a = 0
            v.contracts.forEach((subV, i, typeData) => {
              //展开数据
              const obj = {
                id: v.id,
                level: v.level,
                contract: subV.contract,
                volume:subV.volume,
                long:subV.long,
                short:subV.short
              }
              if (i === 0) {
                obj.typeIndex = typeData.length  //每组个数
              }
              getDate.push(obj)
            })
          }
        })
        this.tableData2 = getDate
        console.log(getDate)
      },
      // 表格合并方法(两层数据)
      arraySpanMethod2({ row, column, rowIndex, columnIndex }) {
        if (columnIndex === 0 || columnIndex === 1 || columnIndex === 9) {
          if (row.typeIndex) { // 如果有值,说明需要合并
            return [row.typeIndex, 1]
          } else return [0, 0]
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .dashboard {
    &-container {
      margin: 30px;
    }
    &-text {
      font-size: 30px;
      line-height: 46px;
    }
  }
  .demo-table-expand {
    font-size: 0;
  }

  .demo-table-expand label {
    width: 90px;
    color: #99a9bf;
  }

  .demo-table-expand .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    width: 20%;
  }
</style>
