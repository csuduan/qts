<script lang="ts">
export default {
  name: "view"
};
</script>

<script setup lang="ts">
import dayjs from "dayjs";
import { getAcctList } from "/@/api/acct";
import { FormInstance,ElMessageBox } from "element-plus";
import { reactive, ref, onMounted } from "vue";
import { EpTableProBar } from "/@/components/ReTable";
import { Switch, message } from "@pureadmin/components";
import { useRenderIcon } from "/@/components/ReIcon/src/hooks";
import { useMultiTagsStoreHook } from "/@/store/modules/multiTags";
import { useRouter, useRoute } from "vue-router";


const route = useRoute();
const router = useRouter();

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

function handleUpdate(row) {
  console.log(row);
}

function jumpDetail(row){
  useMultiTagsStoreHook().handleTags("push", {
    path: `/acct/view/detail`,
    parentPath: route.matched[0].path,
    name: "viewDetail",
    query: { id: String(row.id) },
    meta: {
      title: { zh: `账户-${row.id}`, en: `No.${row.id} - DetailInfo` },
      showLink: false,
      i18n: false,
      dynamicLevel: 3
    }
  });
  router.push({ name: "viewDetail", query: { id: String(row.id) } })
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

function onChange(checked, { $index, row }) {
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

async function onSearch() {
  loading.value = true;
  let { data } = await getAcctList();
  dataList.value = data.list;
  totalPage.value = data.total;
  setTimeout(() => {
    loading.value = false;
  }, 500);
  setInterval(() => {
    if(totalPage.value>0){
      for (let data of dataList.value) {
        //console.log(data)
        //data.balance++;
        //data.mv+=2;
      }
    }
  },500);
}

const resetForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  formEl.resetFields();
  onSearch();
};

onMounted(() => {
  onSearch();
});
</script>

<template>
  <div class="main">


    <EpTableProBar
      title="账户列表"
      :loading="loading"
      :dataList="dataList"
      @refresh="onSearch"
    >
      <template #buttons>
        <el-button type="primary" :icon="useRenderIcon('add')">
          新增
        </el-button>
      </template>
      <template v-slot="{size, checkList }">
        <el-table
          border
          table-layout="auto"
          :size="'small'"
          :data="dataList"
          :header-cell-style="{ background: '#fafafa', color: '#606266' }"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            type="selection"
            align="center"
            width="55"
            prop="enable"
          />
          <el-table-column label="账户分组" align="center" prop="group" />
          <el-table-column label="账户编号" align="center" prop="id" />
          <el-table-column label="账户名称" align="center" prop="name" />
          <el-table-column
            prop="enable"
            label="账户状态"
            width="80"
            :show-overflow-tooltip="true"
            align="center"
          >
            <template #default="scope">
              <el-tag
                :type="scope.row.status == 1 ? 'success' : 'danger'"
                disable-transitions
              >
                {{ scope.row.status == 1 ? "已连接" : "已断开" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="enable"
            label="接口状态"
            width="140"
            :show-overflow-tooltip="true"
            align="center"
          >
            <template #default="scope">
              <el-tag
                :type="scope.row.status == 0 ? 'success' : 'danger'"
                disable-transitions
              >
                交易
              </el-tag>
              <el-divider direction="vertical" />
              <el-tag
                :type="scope.row.status == 1 ? 'success' : 'danger'"
                disable-transitions
              >
                行情
              </el-tag>
            </template>
          </el-table-column>


          <el-table-column label="静态市值" align="center"  prop="balance"/>
          <el-table-column label="动态市值" align="center"  prop="mv"/>
          <el-table-column label="当前保证金" align="center"  prop="margin"/>
          <el-table-column label="风险度" align="center"  prop="risk"/>
          <el-table-column label="持仓盈亏" align="center"  prop="balanceProfit"/>
          <el-table-column label="平仓盈亏" align="center"  prop="closeProfit"/>
          <el-table-column label="手续费" align="center"  prop="fee"/>
          <el-table-column label="更新时间" align="center"  prop="updateTime"/>


<!--          <el-table-column
            label="状态"
            align="center"
            width="130"
            prop="status"
          >
            <template #default="scope">
              <Switch
                :size=" 'small' "
                :loading="switchLoadMap[scope.$index]?.loading"
                v-model:checked="scope.row.enable"
                :checkedValue="1"
                :unCheckedValue="0"
                checked-children="已启用"
                un-checked-children="已禁用"
                @change="checked => onChange(checked, scope)"
              />
            </template>
          </el-table-column>-->
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
                :icon="useRenderIcon('edits')"
              >
                详情
              </el-button>

              <el-dropdown>
                <el-button
                  class="ml-3"
                  type="text"
                  :size="size"
                  @click="handleUpdate(scope.row)"
                  :icon="useRenderIcon('more')"
                />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>
                      <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        :size="size"
                        :icon="useRenderIcon('menu')"
                      >
                        菜单权限
                      </el-button>
                    </el-dropdown-item>
                    <el-dropdown-item>
                      <el-button
                        class="reset-margin !h-20px !text-gray-500"
                        type="text"
                        :size="size"
                        :icon="useRenderIcon('database')"
                      >
                        数据权限
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
      </template>
    </EpTableProBar>
  </div>
</template>

<style scoped lang="scss">
:deep(.el-dropdown-menu__item i) {
  margin: 0;
}
</style>
