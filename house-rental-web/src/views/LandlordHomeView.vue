<template>
  <DashboardShell
    title="出租者首页"
    :role="userInfo?.roleCode || ''"
    :menus="menus"
    :active-menu="activeMenu"
    @menu-click="activeMenu = $event"
    @logout="logout"
  >
    <template #header-actions>
      <el-button v-if="activeMenu === 'houses'" type="primary" @click="openCreateDialog">
        发布房源
      </el-button>
    </template>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card">
          <div class="summary-label">我的房源</div>
          <div class="summary-value">{{ houseList.length }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card">
          <div class="summary-label">待审核房源</div>
          <div class="summary-value warning">{{ pendingHouseCount }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card">
          <div class="summary-label">待处理预约</div>
          <div class="summary-value primary">{{ pendingAppointmentCount }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card">
          <div class="summary-label">待处理报修</div>
          <div class="summary-value success">{{ pendingRepairCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <template v-if="activeMenu === 'houses'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header card-header-between">
            <span>我的房源</span>

            <div class="table-toolbar">
              <el-input
                v-model="houseKeyword"
                clearable
                placeholder="搜索标题/地址"
                class="toolbar-input"
              />
              <el-select
                v-model="houseAuditFilter"
                clearable
                placeholder="审核状态"
                class="toolbar-select"
              >
                <el-option label="待审核" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已拒绝" :value="2" />
              </el-select>
              <el-select
                v-model="houseStatusFilter"
                clearable
                placeholder="房源状态"
                class="toolbar-select"
              >
                <el-option label="已下架" :value="0" />
                <el-option label="已上架" :value="1" />
                <el-option label="待支付" :value="2" />
                <el-option label="已出租" :value="3" />
              </el-select>
              <el-button @click="resetHouseFilters">重置</el-button>
            </div>
          </div>
        </template>

        <div class="table-scroll houses-table">
          <el-table :data="pagedHouseList" border v-loading="loading">
            <el-table-column label="封面" width="110">
              <template #default="scope">
                <el-image
                  v-if="getFirstHouseImage(scope.row)"
                  :src="getFirstHouseImage(scope.row)"
                  fit="cover"
                  class="table-cover"
                  :preview-src-list="parseImageUrls(scope.row.imageUrls)"
                  preview-teleported
                />
                <div v-else class="table-cover placeholder">暂无图片</div>
              </template>
            </el-table-column>

            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="area" label="区域" width="120" />
            <el-table-column prop="rentPrice" label="租金" width="110" />
            <el-table-column prop="houseType" label="户型" width="120" />

            <el-table-column prop="status" label="房源状态" width="110">
              <template #default="scope">
                <el-tag :type="houseStatusTagType(scope.row.status)">
                  {{ formatHouseStatus(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="auditStatus" label="审核状态" width="110">
              <template #default="scope">
                <el-tag :type="auditTagType(scope.row.auditStatus)">
                  {{ formatHouseAuditStatus(scope.row.auditStatus) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="createTime" label="发布时间" min-width="170" />

            <el-table-column label="操作" min-width="290" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="goHouseDetail(scope.row.id)">查看</el-button>
                <el-button type="primary" link @click="openEditDialog(scope.row)">编辑</el-button>

                <el-button
                  v-if="scope.row.status === 1"
                  type="warning"
                  link
                  @click="handleToggleHouseStatus(scope.row, 0)"
                >
                  下架
                </el-button>

                <el-button
                  v-else-if="scope.row.auditStatus === 1 && scope.row.status === 0"
                  type="success"
                  link
                  @click="handleToggleHouseStatus(scope.row, 1)"
                >
                  上架
                </el-button>

                <el-button type="danger" link @click="handleDeleteHouse(scope.row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="housePage"
            v-model:page-size="housePageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="filteredHouseList.length"
          />
        </div>
      </el-card>

      <el-dialog
        v-model="houseDialogVisible"
        :title="houseDialogMode === 'create' ? '发布房源' : '编辑房源'"
        :width="dialogWidth"
        destroy-on-close
      >
        <el-form ref="houseFormRef" :model="houseForm" :rules="houseRules" label-width="100px">
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="房源标题" prop="title">
                <el-input v-model="houseForm.title" placeholder="请输入房源标题" />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-form-item label="户型" prop="houseType">
                <el-input v-model="houseForm.houseType" placeholder="如：两室一厅" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="详细地址" prop="address">
            <el-input v-model="houseForm.address" placeholder="请输入详细地址" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="城市" prop="city">
                <el-input v-model="houseForm.city" placeholder="请输入城市" />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-form-item label="区域" prop="area">
                <el-input v-model="houseForm.area" placeholder="请输入区域" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :xs="24" :md="8">
              <el-form-item label="租金" prop="rentPrice">
                <el-input-number
                  v-model="houseForm.rentPrice"
                  :min="0.01"
                  :precision="2"
                  :step="100"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="8">
              <el-form-item label="押金" prop="deposit">
                <el-input-number
                  v-model="houseForm.deposit"
                  :min="0"
                  :precision="2"
                  :step="100"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="8">
              <el-form-item label="面积" prop="square">
                <el-input-number
                  v-model="houseForm.square"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="楼层" prop="floor">
                <el-input v-model="houseForm.floor" placeholder="如：8/18" />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="6">
              <el-form-item label="经度" prop="longitude">
                <el-input-number
                  v-model="houseForm.longitude"
                  :precision="6"
                  :step="0.000001"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="6">
              <el-form-item label="纬度" prop="latitude">
                <el-input-number
                  v-model="houseForm.latitude"
                  :precision="6"
                  :step="0.000001"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="房源描述" prop="description">
            <el-input
              v-model="houseForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入房源描述"
            />
          </el-form-item>

          <el-form-item label="房源图片">
            <el-upload
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              multiple
              @change="handleHouseImageChange"
            >
              <el-button type="primary" plain>上传图片</el-button>
            </el-upload>

            <div class="upload-tip">
              支持 jpg/png/webp 等图片格式，上传后将保存为服务器文件地址。
            </div>

            <div class="image-preview-grid" v-if="houseForm.imageList.length">
              <div
                v-for="(item, index) in houseForm.imageList"
                :key="`${item}-${index}`"
                class="preview-item"
              >
                <el-image
                  :src="item"
                  fit="cover"
                  class="preview-image"
                  :preview-src-list="houseForm.imageList"
                  preview-teleported
                />
                <el-button
                  size="small"
                  type="danger"
                  circle
                  class="preview-remove"
                  @click="removeHouseImage(index)"
                >
                  ×
                </el-button>
              </div>
            </div>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="houseDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="houseSubmitting" @click="submitHouseForm">
            {{ houseDialogMode === 'create' ? '提交发布' : '保存修改' }}
          </el-button>
        </template>
      </el-dialog>
    </template>

    <template v-if="activeMenu === 'appointments'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">预约管理</div>
        </template>

        <div class="table-scroll">
          <el-table :data="pagedAppointmentList" border v-loading="loading">
            <el-table-column prop="id" label="预约ID" width="100" />
            <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
            <el-table-column prop="tenantName" label="租客名称" width="140" />
            <el-table-column prop="appointmentTime" label="预约时间" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="180" />

            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="success">已同意</el-tag>
                <el-tag v-else type="danger">已拒绝</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180">
              <template #default="scope">
                <template v-if="scope.row.status === 0">
                  <el-button type="success" link @click="handleApprove(scope.row.id)">同意</el-button>
                  <el-button type="danger" link @click="handleReject(scope.row.id)">拒绝</el-button>
                </template>
                <span v-else>--</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination v-model:current-page="appointmentPage" v-model:page-size="appointmentPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="appointmentTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'applications'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">申请管理</div>
        </template>

        <div class="table-scroll">
          <el-table :data="pagedApplicationList" border v-loading="loading">
            <el-table-column prop="id" label="申请ID" width="100" />
            <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
            <el-table-column prop="tenantName" label="租客名称" width="140" />
            <el-table-column prop="remark" label="备注" min-width="220" />

            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="success">已同意</el-tag>
                <el-tag v-else type="danger">已拒绝</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180">
              <template #default="scope">
                <template v-if="scope.row.status === 0">
                  <el-button type="success" link @click="handleApproveApplication(scope.row.id)">
                    同意
                  </el-button>
                  <el-button type="danger" link @click="handleRejectApplication(scope.row.id)">
                    拒绝
                  </el-button>
                </template>
                <span v-else>--</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination v-model:current-page="applicationPage" v-model:page-size="applicationPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="applicationTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'contracts'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">合同管理</div>
        </template>

        <div class="table-scroll">
          <el-table :data="pagedContractList" border v-loading="loading">
            <el-table-column prop="id" label="合同ID" width="100" />
            <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
            <el-table-column prop="tenantName" label="租客名称" width="140" />
            <el-table-column prop="applicationId" label="申请ID" width="100" />
            <el-table-column prop="startDate" label="开始日期" width="140" />
            <el-table-column prop="endDate" label="结束日期" width="140" />
            <el-table-column prop="monthlyRent" label="月租金" width="120" />
            <el-table-column prop="deposit" label="押金" width="120" />

            <el-table-column label="合同附件" min-width="190">
              <template #default="scope">
                <div class="table-inline-actions">
                  <el-button v-if="scope.row.contractUrl" type="primary" link @click="openFile(scope.row.id)">查看附件</el-button>
                  <el-upload action="#" :auto-upload="false" :show-file-list="false" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" @change="(uploadFile) => handleContractUpload(uploadFile, scope.row)">
                    <el-button type="success" link>上传附件</el-button>
                  </el-upload>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="合同状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="warning">待生效</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="success">生效中</el-tag>
                <el-tag v-else-if="scope.row.status === 2" type="info">已结束</el-tag>
                <el-tag v-else type="danger">已取消</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 1"
                  type="danger"
                  link
                  @click="handleFinishContract(scope.row)"
                >
                  结束合同
                </el-button>
                <span v-else>--</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination v-model:current-page="contractPage" v-model:page-size="contractPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="contractTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'orders'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">订单管理</div>
        </template>

        <div class="table-scroll">
          <el-table :data="pagedOrderList" border v-loading="loading">
            <el-table-column prop="id" label="订单ID" width="100" />
            <el-table-column prop="contractId" label="合同ID" width="100" />
            <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
            <el-table-column prop="tenantName" label="租客名称" width="140" />
            <el-table-column prop="amount" label="订单金额" width="120" />

            <el-table-column prop="payType" label="支付方式" width="120">
              <template #default="scope">
                <span>{{ formatPayType(scope.row.payType) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="payStatus" label="支付状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.payStatus === 0" type="warning">未支付</el-tag>
                <el-tag v-else-if="scope.row.payStatus === 1" type="success">已支付</el-tag>
                <el-tag v-else-if="scope.row.payStatus === 2" type="info">已取消</el-tag>
                <el-tag v-else type="danger">已过期</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="payTime" label="支付时间" min-width="180">
              <template #default="scope">
                <span>{{ scope.row.payTime || '--' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination v-model:current-page="orderPage" v-model:page-size="orderPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="orderTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'repairs'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">报修处理</div>
        </template>

        <div class="table-scroll">
          <el-table :data="pagedRepairList" border v-loading="loading">
            <el-table-column prop="id" label="报修ID" width="100" />
            <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
            <el-table-column prop="tenantName" label="租客名称" width="140" />
            <el-table-column prop="content" label="报修内容" min-width="220" />

            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
                <el-tag v-else type="success">已处理</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="result" label="处理结果" min-width="220" />

            <el-table-column label="操作" width="160">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 0"
                  type="primary"
                  link
                  @click="openRepairDialog(scope.row)"
                >
                  处理报修
                </el-button>
                <span v-else>--</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="table-pagination">
          <el-pagination v-model:current-page="repairPage" v-model:page-size="repairPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="repairTotal" />
        </div>
      </el-card>

      <el-dialog v-model="repairDialogVisible" title="处理报修" :width="repairDialogWidth">
        <el-form label-width="90px">
          <el-form-item label="报修ID">
            <el-input :model-value="currentRepair?.id || ''" disabled />
          </el-form-item>

          <el-form-item label="处理结果">
            <el-input
              v-model="repairResultForm.result"
              type="textarea"
              :rows="4"
              placeholder="请输入处理结果"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="repairDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleProcessRepair">提交处理</el-button>
        </template>
      </el-dialog>
    </template>
  </DashboardShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { logoutApi } from '../api/auth'
import DashboardShell from '../components/DashboardShell.vue'
import {
  addHouseApi,
  deleteHouseApi,
  getMyHouseListApi,
  updateHouseApi,
  updateHouseStatusApi,
} from '../api/house'
import {
  getLandlordAppointmentListApi,
  approveAppointmentApi,
  rejectAppointmentApi,
} from '../api/appointment'
import {
  getLandlordApplicationListApi,
  approveRentalApplicationApi,
  rejectRentalApplicationApi,
} from '../api/application'
import { getLandlordRepairListApi, processRepairApi } from '../api/repair'
import { downloadContractFileApi, finishContractLandlordApi, getLandlordContractListApi, updateLandlordContractFileApi } from '../api/contract'
import { getLandlordOrderListApi } from '../api/order'
import { uploadHouseImageApi, uploadLandlordContractFileApi } from '../api/upload'
import {
  formatHouseAuditStatus,
  formatHouseStatus,
  getFirstHouseImage,
  parseImageUrls,
} from '../utils/house'
import { usePagination } from '../composables/usePagination'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const houseList = ref([])
const appointmentList = ref([])
const applicationList = ref([])
const contractList = ref([])
const orderList = ref([])
const repairList = ref([])
const loading = ref(false)
const activeMenu = ref('houses')

const screenWidth = ref(window.innerWidth)
const isMobile = computed(() => screenWidth.value < 768)
const dialogWidth = computed(() => (isMobile.value ? '94%' : '860px'))
const repairDialogWidth = computed(() => (isMobile.value ? '92%' : '520px'))

const updateViewport = () => {
  screenWidth.value = window.innerWidth
}

const menus = [
  { key: 'houses', label: '我的房源' },
  { key: 'appointments', label: '预约管理' },
  { key: 'applications', label: '申请管理' },
  { key: 'contracts', label: '合同管理' },
  { key: 'orders', label: '订单管理' },
  { key: 'repairs', label: '报修处理' },
]

const pendingHouseCount = computed(() =>
  houseList.value.filter((item) => item.auditStatus === 0).length,
)
const pendingAppointmentCount = computed(() =>
  appointmentList.value.filter((item) => item.status === 0).length,
)
const pendingRepairCount = computed(() =>
  repairList.value.filter((item) => item.status === 0).length,
)

const houseKeyword = ref('')
const houseAuditFilter = ref('')
const houseStatusFilter = ref('')
const housePage = ref(1)
const housePageSize = ref(5)

const filteredHouseList = computed(() =>
  houseList.value.filter((item) => {
    const keyword = houseKeyword.value.trim()
    const matchKeyword =
      !keyword ||
      item.title?.includes(keyword) ||
      item.address?.includes(keyword)
    const matchAudit =
      houseAuditFilter.value === '' || item.auditStatus === houseAuditFilter.value
    const matchStatus =
      houseStatusFilter.value === '' || item.status === houseStatusFilter.value
    return matchKeyword && matchAudit && matchStatus
  }),
)

const pagedHouseList = computed(() => {
  const start = (housePage.value - 1) * housePageSize.value
  return filteredHouseList.value.slice(start, start + housePageSize.value)
})

const { currentPage: appointmentPage, pageSize: appointmentPageSize, total: appointmentTotal, pagedList: pagedAppointmentList } = usePagination(appointmentList, 5)
const { currentPage: applicationPage, pageSize: applicationPageSize, total: applicationTotal, pagedList: pagedApplicationList } = usePagination(applicationList, 5)
const { currentPage: contractPage, pageSize: contractPageSize, total: contractTotal, pagedList: pagedContractList } = usePagination(contractList, 5)
const { currentPage: orderPage, pageSize: orderPageSize, total: orderTotal, pagedList: pagedOrderList } = usePagination(orderList, 5)
const { currentPage: repairPage, pageSize: repairPageSize, total: repairTotal, pagedList: pagedRepairList } = usePagination(repairList, 5)

const houseDialogVisible = ref(false)
const houseDialogMode = ref('create')
const houseSubmitting = ref(false)
const houseFormRef = ref()

const createHouseForm = () => ({
  id: null,
  title: '',
  address: '',
  city: '',
  area: '',
  rentPrice: null,
  deposit: 0,
  houseType: '',
  square: null,
  floor: '',
  description: '',
  longitude: null,
  latitude: null,
  imageList: [],
})

const houseForm = reactive(createHouseForm())

const houseRules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  rentPrice: [{ required: true, message: '请输入租金', trigger: 'change' }],
}

const repairDialogVisible = ref(false)
const currentRepair = ref(null)
const repairResultForm = ref({ result: '' })

const resetHouseFilters = () => {
  houseKeyword.value = ''
  houseAuditFilter.value = ''
  houseStatusFilter.value = ''
  housePage.value = 1
}

const resetHouseForm = () => {
  Object.assign(houseForm, createHouseForm())
}

const loadData = async () => {
  if (!userInfo.value?.id) return

  loading.value = true
  try {
    const [houseRes, appointmentRes, applicationRes, contractRes, orderRes, repairRes] =
      await Promise.all([
        getMyHouseListApi(userInfo.value.id),
        getLandlordAppointmentListApi(userInfo.value.id),
        getLandlordApplicationListApi(userInfo.value.id),
        getLandlordContractListApi(userInfo.value.id),
        getLandlordOrderListApi(userInfo.value.id),
        getLandlordRepairListApi(userInfo.value.id),
      ])

    houseList.value = houseRes.data || []
    appointmentList.value = appointmentRes.data || []
    applicationList.value = applicationRes.data || []
    contractList.value = contractRes.data || []
    orderList.value = orderRes.data || []
    repairList.value = repairRes.data || []
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  resetHouseForm()
  houseDialogMode.value = 'create'
  houseDialogVisible.value = true
}

const openEditDialog = (row) => {
  resetHouseForm()
  houseDialogMode.value = 'edit'
  Object.assign(houseForm, {
    id: row.id,
    title: row.title,
    address: row.address,
    city: row.city,
    area: row.area,
    rentPrice: row.rentPrice,
    deposit: row.deposit,
    houseType: row.houseType,
    square: row.square,
    floor: row.floor,
    description: row.description,
    longitude: row.longitude,
    latitude: row.latitude,
    imageList: parseImageUrls(row.imageUrls),
  })
  houseDialogVisible.value = true
}

const normalizeFileUrl = (url) => {
  if (!url) return ''

  let text = String(url).trim()
  if (!text) return ''

  // 如果后端返回完整地址，例如 http://localhost:8080/uploads/xxx.jpg，
  // 保存到数据库时只保留 /uploads/xxx.jpg，方便前端代理和迁移部署。
  if (text.startsWith('http://') || text.startsWith('https://')) {
    try {
      const urlObj = new URL(text)
      text = urlObj.pathname
    } catch {
      console.warn('图片地址解析失败，保留原始地址：', text)
    }
  }

  return text
}

const extractUploadUrl = (res) => {
  if (!res) return ''

  // 情况1：接口直接返回字符串
  if (typeof res === 'string') {
    return normalizeFileUrl(res)
  }

  // 情况2：后端把上传地址放在 message 中
  // 你的当前返回就是这种：
  // { code: 200, data: null, message: "/uploads/images/xxx.jpg" }
  if (typeof res.message === 'string' && res.message.includes('/uploads/')) {
    return normalizeFileUrl(res.message)
  }

  // 情况3：request 封装后，res.data 是字符串
  if (typeof res.data === 'string') {
    return normalizeFileUrl(res.data)
  }

  // 情况4：Axios 原始响应，res.data.message 是上传地址
  if (typeof res.data?.message === 'string' && res.data.message.includes('/uploads/')) {
    return normalizeFileUrl(res.data.message)
  }

  // 情况5：Axios 原始响应，res.data.data 是字符串
  if (typeof res.data?.data === 'string') {
    return normalizeFileUrl(res.data.data)
  }

  // 情况6：后端返回对象，里面可能有 url / fileUrl / path
  const possibleUrl =
    res.url ||
    res.fileUrl ||
    res.path ||
    res.message ||
    res.data?.url ||
    res.data?.fileUrl ||
    res.data?.path ||
    res.data?.message ||
    res.data?.data?.url ||
    res.data?.data?.fileUrl ||
    res.data?.data?.path ||
    res.data?.data?.message

  return normalizeFileUrl(possibleUrl)
}

const buildImageUrlsForSubmit = () => {
  const urls = (houseForm.imageList || [])
    .map((item) => {
      if (typeof item === 'string') {
        return normalizeFileUrl(item)
      }

      if (item?.url) {
        return normalizeFileUrl(item.url)
      }

      if (item?.response) {
        return extractUploadUrl(item.response)
      }

      return ''
    })
    .filter(Boolean)

  return JSON.stringify([...new Set(urls)])
}

const handleHouseImageChange = async (uploadFile) => {
  if (!uploadFile?.raw) return

  try {
    const res = await uploadHouseImageApi(uploadFile.raw)
    const imageUrl = extractUploadUrl(res)

    if (!imageUrl) {
      console.log('图片上传接口返回：', res)
      ElMessage.error('图片上传成功，但未获取到图片地址')
      return
    }

    if (!houseForm.imageList.includes(imageUrl)) {
      houseForm.imageList.push(imageUrl)
    }

    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('图片上传失败')
  }
}

const handleContractUpload = async (uploadFile, row) => {
  console.log('上传按钮触发了')
  console.log('uploadFile = ', uploadFile)
  console.log('uploadFile.raw = ', uploadFile?.raw)
  console.log('row = ', row)

  const realFile = uploadFile?.raw || uploadFile

  if (!realFile) {
    ElMessage.error('请选择要上传的文件')
    return
  }

  if (realFile.size <= 0) {
    ElMessage.error('文件内容为空，请选择正常的合同附件')
    return
  }

  if (realFile.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }

  if (!row?.id) {
    ElMessage.error('合同信息不存在，无法上传附件')
    return
  }

  try {
    const uploadRes = await uploadLandlordContractFileApi(realFile)
    const fileUrl = extractUploadUrl(uploadRes)

    if (!fileUrl) {
      console.log('合同附件上传接口返回：', uploadRes)
      ElMessage.error('合同附件上传成功，但未获取到文件地址')
      return
    }

    await updateLandlordContractFileApi(row.id, fileUrl)
    ElMessage.success('合同附件上传成功')
    await loadData()
  } catch (error) {
    console.log('合同附件上传失败：', error)
    ElMessage.error(error?.message || '合同附件上传失败')
  }
}

const openFile = async (contractId) => {
  if (!contractId) return
  try {
    const blob = await downloadContractFileApi(contractId)
    const fileUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = fileUrl
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(fileUrl), 60000)
  } catch (error) {
    console.log('合同附件打开失败：', error)
  }
}

const removeHouseImage = (index) => {
  houseForm.imageList.splice(index, 1)
}

const submitHouseForm = async () => {
  await houseFormRef.value.validate()
  houseSubmitting.value = true
  try {
    const payload = {
      title: houseForm.title,
      address: houseForm.address,
      city: houseForm.city,
      area: houseForm.area,
      rentPrice: houseForm.rentPrice,
      deposit: houseForm.deposit,
      houseType: houseForm.houseType,
      square: houseForm.square,
      floor: houseForm.floor,
      description: houseForm.description,
      longitude: houseForm.longitude,
      latitude: houseForm.latitude,
      imageUrls: buildImageUrlsForSubmit(),
    }

    if (houseDialogMode.value === 'create') {
      await addHouseApi(payload)
      ElMessage.success('房源发布成功，等待管理员审核')
    } else {
      await updateHouseApi(houseForm.id, payload)
      ElMessage.success('房源修改成功，已重新进入审核流程')
    }

    houseDialogVisible.value = false
    await loadData()
  } finally {
    houseSubmitting.value = false
  }
}

const handleToggleHouseStatus = async (row, status) => {
  const actionText = status === 1 ? '上架' : '下架'
  await ElMessageBox.confirm(`确定要${actionText}“${row.title}”吗？`, '提示', {
    type: 'warning',
  })
  await updateHouseStatusApi(row.id, status)
  ElMessage.success(`房源已${actionText}`)
  await loadData()
}

const handleDeleteHouse = async (row) => {
  await ElMessageBox.confirm(`确定删除“${row.title}”吗？删除后不可恢复。`, '提示', {
    type: 'warning',
  })
  await deleteHouseApi(row.id)
  ElMessage.success('房源删除成功')
  await loadData()
}

const goHouseDetail = (id) => {
  router.push({ path: `/house/${id}`, query: { scope: 'landlord' } })
}

const auditTagType = (auditStatus) => {
  if (auditStatus === 0) return 'warning'
  if (auditStatus === 1) return 'success'
  return 'danger'
}

const houseStatusTagType = (status) => {
  if (status === 0) return 'info'
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  return 'danger'
}

const formatPayType = (payType) => {
  if (payType === 'ALIPAY') return '支付宝'
  if (payType === 'WECHAT') return '微信支付'
  return payType || '--'
}

const handleApprove = async (id) => {
  await approveAppointmentApi(id)
  ElMessage.success('已同意预约')
  await loadData()
}

const handleReject = async (id) => {
  await rejectAppointmentApi(id)
  ElMessage.success('已拒绝预约')
  await loadData()
}

const handleApproveApplication = async (id) => {
  await approveRentalApplicationApi(id)
  ElMessage.success('已同意申请')
  await loadData()
}

const handleRejectApplication = async (id) => {
  await rejectRentalApplicationApi(id)
  ElMessage.success('已拒绝申请')
  await loadData()
}

const handleFinishContract = async (row) => {
  await ElMessageBox.confirm(`确定结束合同 ${row.id} 吗？结束后房源会恢复为下架状态。`, '提示', {
    type: 'warning',
  })
  await finishContractLandlordApi(row.id)
  ElMessage.success('合同已结束')
  await loadData()
}

const openRepairDialog = (row) => {
  currentRepair.value = row
  repairResultForm.value = { result: row.result || '' }
  repairDialogVisible.value = true
}

const handleProcessRepair = async () => {
  if (!currentRepair.value?.id) return
  if (!repairResultForm.value.result) {
    ElMessage.warning('请输入处理结果')
    return
  }

  await processRepairApi(currentRepair.value.id, {
    status: 1,
    result: repairResultForm.value.result,
  })

  ElMessage.success('报修处理成功')
  repairDialogVisible.value = false
  currentRepair.value = null
  repairResultForm.value = { result: '' }
  await loadData()
}

const logout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.error(error)
  } finally {
    userStore.clearUserInfo()
    router.push('/login')
  }
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.summary-row {
  margin-bottom: 20px;
}

.summary-card {
  text-align: center;
}

.summary-label {
  font-size: 15px;
  color: #606266;
  margin-bottom: 10px;
}

.summary-value {
  font-size: 30px;
  font-weight: 700;
  color: #303133;
}

.summary-value.warning {
  color: #e6a23c;
}

.summary-value.primary {
  color: #409eff;
}

.summary-value.success {
  color: #67c23a;
}

.section-card {
  border-radius: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 700;
}

.card-header-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.table-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.toolbar-input {
  width: 220px;
}

.toolbar-select {
  width: 140px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 920px;
}

.houses-table :deep(.el-table) {
  min-width: 1320px;
}

.table-cover {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
}

.table-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f4f5;
  color: #909399;
  font-size: 12px;
}

.table-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.upload-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.image-preview-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 14px;
}

.preview-item {
  position: relative;
}

.preview-image {
  width: 100%;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
}

.preview-remove {
  position: absolute;
  top: 6px;
  right: 6px;
}

@media (max-width: 767px) {
  .summary-value {
    font-size: 26px;
  }

  .card-header-between {
    flex-direction: column;
    align-items: stretch;
  }

  .table-toolbar {
    justify-content: stretch;
  }

  .toolbar-input,
  .toolbar-select {
    width: 100%;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .image-preview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-card :deep(.el-card__body) {
    padding: 14px;
  }

  .section-card :deep(.el-card__header) {
    padding: 16px 14px;
  }

  .section-card :deep(.el-form-item__label) {
    width: 90px !important;
  }
}
</style>
