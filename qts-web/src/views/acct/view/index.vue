<script setup lang="ts">
import {acctOperate, getAcctInstList, stopAcctInst,startAcctInst} from "@/api/acct";
import {FormInstance, ElMessageBox} from "element-plus";
import {reactive, ref, onMounted} from "vue";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import {useMultiTagsStoreHook} from "@/store/modules/multiTags";
import {useRouter, useRoute} from "vue-router";
import {useAcctStoreHook} from "@/store/modules/acct";
import {MsgType} from "@/utils/enums";
import {clone, delay} from "@pureadmin/utils";
import type {PaginationProps, LoadingConfig, Align} from "@pureadmin/table";
import More2Fill from "@iconify-icons/ri/more-2-fill";
import Delete from "@iconify-icons/ep/delete";
import EditPen from "@iconify-icons/ep/edit-pen";
import Search from "@iconify-icons/ep/search";
import Refresh from "@iconify-icons/ep/refresh";
import Menu from "@iconify-icons/ep/menu";
import Connect from "@iconify-icons/ep/connection";
import AddFill from "@iconify-icons/ri/add-circle-line";
import { useDetail } from "../hooks";


defineOptions({
  name: "view"
});


//const route = useRoute();
//const router = useRouter();
const { toDetail } = useDetail();


const form = reactive({
  name: "",
  code: "",
  status: ""
});
let dataList = ref([]);
let pageSize = ref(10);
let totalPage = ref(0);
let loading = ref(true);
let switchLoadMap = ref({});

const formRef = ref<FormInstance>();


const statusMap = {
  'READY': '已就绪',
  'UNSTARTED': '未启动',
  'STARTING': '启动中',
  'CONNING':'连接中'
};

const getStatusName=(status)=>{
  return statusMap[status];
}

function handleUpdate(row) {
}

function connectAcct(row, status) {
  let acctId = row.id;
  console.log("connectAcct", acctId);
  let data = {
    status: status
  };
  let msg = {
    type: MsgType.CONNECT,
    acctId: acctId,
    data: {
      status: status
    }
  };
  console.log(msg);
  acctOperate(msg);
}

function stopAcct(row){
  let acctId = row.id;
  stopAcctInst({acctId:acctId})
}

function startAcct(row){
  let acctId = row.id;
  startAcctInst({acctId:acctId})
}

function jumpDetail(row) {
  var acctId=row.id;
  var params={id: String(acctId)}
   toDetail(params)

  // useMultiTagsStoreHook().handleTags("push", {
  //   path: `/acct/detail`,
  //   parentPath: route.matched[0].path,
  //   name: "acct-detail",
  //   query: params,
  //   meta: {
  //     title: `账户-${params.id}`,
  //     dynamicLevel: 3
  //   }
  // });
  // router.push({name: "acct-detail", query: params});
}

function handleDelete(row) {
  console.log(row);
}

function handleCurrentChange(val: number) {
  console.log(`current page: ${val}`);
}

function handleSizeChange(val: number) {
  console.log(`${val} items per page`);
}

function handleSelectionChange(val) {
  console.log("handleSelectionChange", val);
}



function onChange(checked, {$index, row}) {
  ElMessageBox.confirm(
      `确认要<strong>${
          row.status === 0 ? "停用" : "启用"
      }</strong><strong style='color:var(--el-color-primary)'>${
          row.name
      }</strong>角色吗?`,
      "系统提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
        dangerouslyUseHTMLString: true,
        draggable: true
      }
  )
      .then(() => {
        switchLoadMap.value[$index] = Object.assign(
            {},
            switchLoadMap.value[$index],
            {
              loading: true
            }
        );
        setTimeout(() => {
          switchLoadMap.value[$index] = Object.assign(
              {},
              switchLoadMap.value[$index],
              {
                loading: false
              }
          );
          message.success("已成功修改角色状态");
        }, 300);
      })
      .catch(() => {
        row.status === 0 ? (row.status = 1) : (row.status = 0);
      });
}

async function fetchAcctInfos() {
  console.info("fetchAcctInfos...")
  loading.value = true;
  let {data} = await getAcctInstList();
  dataList.value = data;
  totalPage.value = data.length;
  useAcctStoreHook().setAcctInfos(data);
  // setTimeout(() => {
  //   loading.value = false;
  // }, 500);
}

const resetForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  formEl.resetFields();
  fetchAcctInfos();
};

onMounted(() => {
  fetchAcctInfos();
  // 创建一个每60秒触发一次的计时器
  const refreshInterval = setInterval(() => {
     fetchAcctInfos(); // 定时获取数据并更新状态
  }, 60000); // 60秒


});



</script>

<template>
  <div class="main">
    <div>
      <el-button type="primary" @click="startAcct" size="small">启动</el-button>
      <el-button type="primary" @click="stopAcct" size="small">停止</el-button>
      <el-button type="primary" @click="stopAcct" size="small">连接</el-button>
      <el-button type="primary" @click="stopAcct" size="small">断开</el-button>

    </div>
    <div class="acct-list">
      <el-table
          border
          table-layout="auto"
          :size="'small'"
          :data="useAcctStoreHook().acctInfos"
          :header-cell-style="{ background: '#fafafa', color: '#606266' }"
      >
        <el-table-column
            type="selection"
            align="center"
            width="55"
            prop="isSelected"
        />
        <el-table-column label="分组" align="center" prop="group"/>
        <el-table-column label="账户编号" align="center" prop="id"/>
        <el-table-column
            label="账户状态"
            width="80"
            :show-overflow-tooltip="true"
            align="center"
        >
          <template #default="scope">
            <el-tag
                :type="scope.row.status == 'READY' ? 'success' : 'danger'"
                disable-transitions
            >
              {{ getStatusName(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
            label="接口状态"
            width="140"
            :show-overflow-tooltip="true"
            align="center"
        >
          <template #default="scope">
            <el-tag
                :type="scope.row.tdStatus == 1 ? 'success' : 'danger'"
                disable-transitions
            >
              交易
            </el-tag>
            <el-divider direction="vertical"/>
            <el-tag
                :type="scope.row.mdStatus == 1 ? 'success' : 'danger'"
                disable-transitions
            >
              行情
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="静态市值" align="center" prop="balance"/>
        <el-table-column label="动态市值" align="center" prop="mv"/>
        <el-table-column label="当前保证金" align="center" prop="margin"/>
        <el-table-column
            label="持仓盈亏"
            align="center"
            prop="balanceProfit"
        />
        <el-table-column label="平仓盈亏" align="center" prop="closeProfit"/>
        <el-table-column label="手续费" align="center" prop="fee"/>
        <el-table-column label="更新时间" align="center" prop="updateTimes"/>
        <el-table-column
            fixed="right"
            label="操作"
            width="120"
            align="center"
        >
          <template #default="scope">
            <el-button
                class="reset-margin"
                type="text"
                :size="size"
                @click="jumpDetail(scope.row)"
                :icon="useRenderIcon(Menu)"
            >
              详情
            </el-button>

            <el-dropdown>
              <el-button
                  class="ml-3"
                  type="text"
                  :size="size"
                  @click="handleUpdate(scope.row)"
                  :icon="useRenderIcon(More2Fill)"
              />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        @click="startAcct(scope.row)"
                        :size="size"
                    >
                      启动
                    </el-button>
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        @click="stopAcct(scope.row)"
                        :size="size"
                    >
                      停止
                    </el-button>
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        @click="connectAcct(scope.row, false)"
                        :size="size"
                    >
                      断开
                    </el-button>
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        @click="connectAcct(scope.row, true)"
                        :size="size"
                    >
                      连接
                    </el-button>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
          class="flex justify-end mt-4"
          :small="size === 'small' ? true : false"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :background="true"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
:deep(.el-dropdown-menu__item i) {
  margin: 0;
}

.acct-list{
  margin-top: 10px;
}
</style>
