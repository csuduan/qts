<template>
  <div class="app-container">
    <el-table v-loading="listLoading"
      :data="tasks"
      border
      fit
      highlight-current-row
      style="width: 100%">
      <el-table-column
        label="分组"
        prop="group" width="60">
      </el-table-column>
      <el-table-column
        label="任务名"
        prop="name">
      </el-table-column>
      <el-table-column
        label="任务描述"
        prop="desc">
      </el-table-column>
      <el-table-column
        label="状态"
        prop="status">
      </el-table-column>
      <el-table-column
        label="Cron表达式"
        prop="cron">
      </el-table-column>
      <el-table-column
        label="上一次触发"
        prop="beforFireTime">
      </el-table-column>
      <el-table-column
        label="下一次触发"
        prop="nextFireTime">
      </el-table-column>
      <el-table-column label="Actions" align="center" width="250" class-name="small-padding fixed-width">
        <template slot-scope="{row,$index}">
          <el-button type="primary" size="mini" @click="onSubmit(row)">
            手动触发
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

</template>

<script>
  import { fetchList } from '@/api/task'
    export default {
      name: "setting-task.vue",
      data() {
        return {
          listLoading: false,
          tasks:[
            {
              group:'qts',
              name:'autoConeectMdtask',
              desc:'自动连接行情任务',
              beforFireTime:'21:59:59',
              nextFireTime:'21:59:59',
              cron:'0 * * * * *'
            }
          ]
        }
      },
      created() {
        this.getTaskList()
      },
      methods: {
        onSubmit() {
          this.listLoading = false
          fetchList(this.listQuery).then(response => {
            //this.list = response.data.items
            //this.tasks = response.data.total
            this.listLoading = false
          })
        },
        getTaskList() {
          this.listLoading = false
          fetchList().then(response => {
            this.tasks = response.data
            // Just to simulate the time of the request
            setTimeout(() => {
              //this.listLoading = false
            }, 1.5 * 1000)
          })
        },
      }

    }
</script>

<style scoped>

</style>
