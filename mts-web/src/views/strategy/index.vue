<template>
  <div class="app-container">
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
  </div>
</template>

<script>
  export default {
    data() {
      return {
        tableData: [],
        tableData2: []
      }
    },
    created() {
      this.getTable()
    },
    methods: {
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
        // 0: {id: 1, name: "器械1", nameIndex: 5, region: "中国", type: "器械", typeIndex: 2},
        // 1: {id: 1, name: "器械2", region: "中国", type: "器械"}
        // ....
        // 5: {id: 2, name: "器械1", nameIndex: 4, region: "美国", type: "器械", typeIndex: 2}
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

<style scoped>

</style>
