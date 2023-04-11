<template>
  <div class="app-container">
    <el-button style="margin-bottom: 10px; float:right" type="primary" align="right" icon="el-icon-edit">
      添加
    </el-button>


    <el-table
      :data="tableData"
      border
      fit
      highlight-current-row
      style="width: 100%">
      <el-table-column type="expand">
        <template slot-scope="props">
          <el-form label-position="left" inline class="demo-table-expand">
            <el-form-item label="账户名称">
              <span>{{ props.row.name }}</span>
            </el-form-item>
            <el-form-item label="实例状态">
              <span>{{ props.row.instStatus }}</span>
            </el-form-item>
            <el-form-item label="经纪商">
              <span>{{ props.row.broker }}</span>
            </el-form-item>
            <el-form-item label="用户ID">
              <span>{{ props.row.userId }}</span>
            </el-form-item>
            <el-form-item label="行情地址">
              <span>{{ props.row.mdAddress }}</span>
            </el-form-item>
            <el-form-item label="交易地址">
              <span>{{ props.row.tdAddress }}</span>
            </el-form-item>
            <el-form-item label="行情状态">
              <span>{{ props.row.mdStatus }}</span>
              <el-button type="danger" style="margin-left:10px" size="mini" @click="handleUpdate(row)">
                断开
              </el-button>
            </el-form-item>
            <el-form-item label="交易状态">
              <span>{{ props.row.tdStatus }}</span>
              <el-button type="success" style="margin-left:10px" size="mini" @click="handleUpdate(row)">
                连接
              </el-button>
            </el-form-item>
          </el-form>
        </template>
      </el-table-column>
      <el-table-column
        label="账户名称"
        prop="name">
      </el-table-column>
      <el-table-column
        label="实例状态"
        prop="instStatus">
      </el-table-column>
      <el-table-column
        label="账户描述"
        prop="desc">
      </el-table-column>
      <el-table-column label="Actions" align="center" width="250" class-name="small-padding fixed-width">
        <template slot-scope="{row,$index}">
          <el-button type="primary" size="mini" @click="handleUpdate(row)">
            启动
          </el-button>
          <el-button v-if="row.status!='deleted'" size="mini" type="danger" @click="handleDelete(row,$index)">
            关闭
          </el-button>
          <el-dropdown size="mini" split-button type="primary" style="margin-left:10px">
            更多
            <el-dropdown-menu slot="dropdown" >
              <el-dropdown-item @click.native.prevent="handleEdit(row,$index)">编辑</el-dropdown-item>
              <el-dropdown-item @click.native.prevent="handleDelete(row,$index)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>


        </template>
      </el-table-column>

    </el-table>
  </div>

</template>

<style>
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
    width: 50%;
  }
</style>

<script>
  export default {
    data() {
      return {
        tableData: [{
          name: '上期仿真-DQ',
          instStatus: '已启动',
          desc: '测试账户',
          broker:'9999',
          userId:'47008',
          mdAddress:'ctp|tcp://10.80.118.101:20100',
          tdAddress:'ctp|tcp://10.80.118.101:20101',
          mdStatus: '连接',
          tdStatus: '断开'
        }, {
          name: '上期24-DQ',
          instStatus: '未启动',
          desc: '测试账户',broker:'9999',
          broker:'9999',
          userId:'47008',
          mdAddress:'ctp|tcp://10.80.118.101:20100',
          tdAddress:'ctp|tcp://10.80.118.101:20101',
          mdStatus: '连接',
          tdStatus: '连接'
        }, {
          name: '华泰主席-ZLZ',
          instStatus: '已启动',
          desc: '测试账户',
          broker:'9999',
          userId:'47007',
          mdAddress:'ctp|tcp://10.80.118.101:20100',
          tdAddress:'ctp|tcp://10.80.118.101:20101',
          mdStatus: '断开',
          tdStatus: '断开'
        }]
      }
    },
    handleUpdate(row) {
      this.temp = Object.assign({}, row) // copy obj
      this.temp.timestamp = new Date(this.temp.timestamp)
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    handleDelete(row, index) {
      this.$notify({
        title: 'Success',
        message: 'Delete Successfully',
        type: 'success',
        duration: 2000
      })
      this.list.splice(index, 1)
    }
  }
</script>
