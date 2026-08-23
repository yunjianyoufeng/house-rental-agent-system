<template>
  <DashboardShell
    title="租客首页"
    :role="userInfo?.roleCode || ''"
    :menus="menus"
    :active-menu="activeMenu"
    @menu-click="handleMenuClick"
    @logout="logout"
  >
    <el-row :gutter="20" class="summary-row">
      <el-col :span="6">
        <el-card class="summary-card">
          <div class="summary-label">我的预约</div>
          <div class="summary-value">{{ appointmentList.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="summary-card">
          <div class="summary-label">我的申请</div>
          <div class="summary-value">{{ applicationList.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="summary-card">
          <div class="summary-label">待支付订单</div>
          <div class="summary-value warning">{{ unpaidOrderCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="summary-card">
          <div class="summary-label">我的投诉</div>
          <div class="summary-value primary">{{ complaintList.length }}</div>
        </el-card>
      </el-col>
    </el-row>

    <template v-if="activeMenu === 'recommend'">
      <el-card class="section-card recommend-card">
        <template #header>
          <div class="card-header card-header-between">
            <div>智能找房</div>
            <el-tag type="primary">{{ currentRecommendModelLabel }}</el-tag>
          </div>
        </template>

        <div class="recommend-intro">
          租客可以输入自然语言形式的租房需求，系统会根据房源的租金、位置、户型、交通、装修和描述等信息进行推荐。
        </div>

        <div class="recommend-form">
          <el-input
            v-model="recommendQuery"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="例如：我想找精装公寓，交通方便，可以直接入住，适合一个人住"
          />

          <div class="recommend-actions">
            <el-select v-model="recommendModelType" class="recommend-model" placeholder="推荐模型">
              <el-option
                v-for="item in recommendModelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>

            <el-select v-model="recommendTopK" class="recommend-topk" placeholder="推荐数量">
              <el-option label="推荐 3 套" :value="3" />
              <el-option label="推荐 5 套" :value="5" />
              <el-option label="推荐 8 套" :value="8" />
            </el-select>

            <el-button type="primary" :loading="recommendLoading" @click="handleRecommend">
              开始推荐
            </el-button>

            <el-button @click="clearRecommend">
              清空
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" v-if="recommendList.length > 0">
        <template #header>
          <div class="card-header">推荐结果</div>
        </template>

        <el-table :data="recommendList" border v-loading="recommendLoading">

          <el-table-column label="房源标题" min-width="180">
            <template #default="scope">
              {{ scope.row.house?.title }}
            </template>
          </el-table-column>

          <el-table-column label="城市/区域" min-width="160">
            <template #default="scope">
              {{ scope.row.house?.city }} / {{ scope.row.house?.area }}
            </template>
          </el-table-column>

          <el-table-column label="户型" width="120">
            <template #default="scope">
              {{ scope.row.house?.houseType || '--' }}
            </template>
          </el-table-column>

          <el-table-column label="租金" width="120">
            <template #default="scope">
              ¥ {{ scope.row.house?.rentPrice }}
            </template>
          </el-table-column>

          <el-table-column label="推荐分" width="120">
            <template #default="scope">
              <el-tag type="danger">{{ Number(scope.row.score || 0).toFixed(1) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="推荐原因" min-width="260">
            <template #default="scope">
              {{ scope.row.reason || '--' }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button type="primary" link @click="goHouseDetail(scope.row.house?.id)">
                查看房源
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-empty
        v-if="!recommendLoading && recommendQuery && recommendList.length === 0"
        description="暂无推荐结果"
      />
    </template>

    <template v-if="activeMenu === 'appointments'">
      <el-card class="section-card">
        <template #header><div class="card-header">我的预约</div></template>
        <el-table :data="pagedAppointmentList" border v-loading="loading">
          <el-table-column prop="id" label="预约ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="landlordName" label="房东名称" width="140" />
          <el-table-column prop="appointmentTime" label="预约时间" min-width="180" />
          <el-table-column prop="remark" label="备注" min-width="220" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="success">已同意</el-tag>
              <el-tag v-else type="danger">已拒绝</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button type="primary" link @click="goHouseDetail(scope.row.houseId)">查看房源</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="appointmentPage" v-model:page-size="appointmentPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="appointmentTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'applications'">
      <el-card class="section-card">
        <template #header><div class="card-header">我的申请</div></template>
        <el-table :data="pagedApplicationList" border v-loading="loading">
          <el-table-column prop="id" label="申请ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="landlordName" label="房东名称" width="140" />
          <el-table-column prop="remark" label="备注" min-width="220" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="success">已同意</el-tag>
              <el-tag v-else type="danger">已拒绝</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button type="primary" link @click="goHouseDetail(scope.row.houseId)">查看房源</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="applicationPage" v-model:page-size="applicationPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="applicationTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'contracts'">
      <el-card class="section-card">
        <template #header><div class="card-header">我的合同</div></template>
        <el-table :data="pagedContractList" border v-loading="loading">
          <el-table-column prop="id" label="合同ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="landlordName" label="房东名称" width="140" />
          <el-table-column prop="monthlyRent" label="月租金" width="120" />
          <el-table-column prop="deposit" label="押金" width="120" />
          <el-table-column prop="startDate" label="开始日期" width="140" />
          <el-table-column prop="endDate" label="结束日期" width="140" />
          <el-table-column label="合同附件" width="120">
            <template #default="scope">
              <el-button v-if="scope.row.contractUrl" type="primary" link @click="openFile(scope.row.id)">查看</el-button>
              <span v-else>--</span>
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
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button type="primary" link @click="goHouseDetail(scope.row.houseId)">查看房源</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="contractPage" v-model:page-size="contractPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="contractTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'orders'">
      <el-card class="section-card">
        <template #header><div class="card-header">我的订单</div></template>
        <el-table :data="pagedOrderList" border v-loading="loading">
          <el-table-column prop="id" label="订单ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="landlordName" label="房东名称" width="140" />
          <el-table-column prop="amount" label="金额" width="120" />
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
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <template v-if="scope.row.payStatus === 0">
                <el-button type="primary" link @click="openPayDialog(scope.row)">去支付</el-button>
                <el-button type="danger" link @click="handleCancelOrder(scope.row)">取消订单</el-button>
              </template>
              <span v-else>--</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="orderPage" v-model:page-size="orderPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="orderTotal" />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'repairs'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header card-header-between">
            <div>我的报修</div>
            <el-button type="primary" @click="repairDialogVisible = true">提交报修</el-button>
          </div>
        </template>

        <el-table :data="pagedRepairList" border v-loading="loading">
          <el-table-column prop="id" label="报修ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="landlordName" label="房东名称" width="140" />
          <el-table-column prop="content" label="报修内容" min-width="220" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="primary">处理中</el-tag>
              <el-tag v-else type="success">已完成</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="result" label="处理结果" min-width="220">
            <template #default="scope">
              <span>{{ scope.row.result || '--' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="repairPage" v-model:page-size="repairPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="repairTotal" />
        </div>
      </el-card>

      <el-dialog v-model="repairDialogVisible" title="提交报修" width="560px">
        <el-form label-width="100px">
          <el-form-item label="选择房源">
            <el-select v-model="repairForm.houseId" placeholder="请选择已签约房源" style="width:100%">
              <el-option
                v-for="item in repairHouseOptions"
                :key="item.houseId"
                :label="item.label"
                :value="item.houseId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="报修内容">
            <el-input v-model="repairForm.content" type="textarea" :rows="4" placeholder="请输入报修内容" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="repairDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddRepair">提交</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-if="activeMenu === 'complaints'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header card-header-between">
            <div>我的投诉</div>
            <el-button type="primary" @click="complaintDialogVisible = true">提交投诉</el-button>
          </div>
        </template>

        <el-table :data="pagedComplaintList" border v-loading="loading">
          <el-table-column prop="id" label="投诉ID" width="100" />
          <el-table-column prop="targetName" label="投诉对象" width="140">
            <template #default="scope">
              <span>{{ scope.row.targetName || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="投诉内容" min-width="240" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else type="success">已处理</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="result" label="处理结果" min-width="220">
            <template #default="scope">
              <span>{{ scope.row.result || '--' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination v-model:current-page="complaintPage" v-model:page-size="complaintPageSize" :page-sizes="[5, 10, 20]" background layout="total, sizes, prev, pager, next" :total="complaintTotal" />
        </div>
      </el-card>

      <el-dialog v-model="complaintDialogVisible" title="提交投诉" width="560px">
        <el-form label-width="100px">
          <el-form-item label="投诉对象">
            <el-select v-model="complaintForm.targetId" clearable placeholder="可选：选择对应房东" style="width:100%">
              <el-option
                v-for="item in complaintTargetOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="投诉内容">
            <el-input v-model="complaintForm.content" type="textarea" :rows="4" placeholder="请输入投诉内容" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="complaintDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddComplaint">提交</el-button>
        </template>
      </el-dialog>
    </template>

    <el-dialog v-model="paymentDialogVisible" title="订单支付" width="560px">
      <div v-if="currentPayOrder" class="payment-dialog-body">
        <div class="payment-order-box">
          <div>订单号：{{ currentPayOrder.id }}</div>
          <div>合同号：{{ currentPayOrder.contractId }}</div>
          <div class="payment-amount">应付金额：¥ {{ currentPayOrder.amount }}</div>
        </div>

        <div class="payment-channel-title">请选择支付方式</div>
        <el-radio-group v-model="selectedPayType" @change="loadPaymentPreview">
          <el-radio-button label="ALIPAY">支付宝</el-radio-button>
          <el-radio-button label="WECHAT">微信支付</el-radio-button>
        </el-radio-group>

        <div class="payment-preview" v-loading="paymentLoading">
          <template v-if="paymentPreview && selectedPayType === 'WECHAT'">
            <div class="wechat-pay-box">
              <img
                v-if="paymentPreview.qrCodeBase64"
                :src="paymentPreview.qrCodeBase64"
                alt="微信支付二维码"
                class="wechat-pay-qrcode"
              />
              <div class="wechat-pay-tip">请使用微信扫一扫完成支付</div>
            </div>
          </template>

          <template v-else-if="paymentPreview && selectedPayType === 'ALIPAY'">
            <div class="cashier-box">
              <div class="cashier-title">{{ paymentPreview.cashierTitle || '支付宝收银台' }}</div>
              <div class="cashier-desc">{{ paymentPreview.cashierDescription }}</div>
              <div class="cashier-tip">点击下方按钮后，再确认本次订单支付成功。</div>
              <el-button type="primary" plain @click="openMockCashier">打开支付宝收银台（演示）</el-button>
            </div>
          </template>

          <el-alert
            v-if="paymentPreview"
            :title="paymentPreview.instruction"
            type="info"
            :closable="false"
            show-icon
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paymentSubmitting" @click="confirmPayment">
          {{ paymentPreview?.demoMode ? '模拟支付成功' : '我已完成支付' }}
        </el-button>
      </template>
    </el-dialog>
  </DashboardShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { logoutApi } from '../api/auth'
import DashboardShell from '../components/DashboardShell.vue'
import { getTenantAppointmentListApi } from '../api/appointment'
import { getTenantApplicationListApi } from '../api/application'
import { downloadContractFileApi, getTenantContractListApi } from '../api/contract'
import { cancelOrderApi, getTenantOrderListApi, payOrderApi, startOrderPaymentApi } from '../api/order'
import { addRepairApi, getTenantRepairListApi } from '../api/repair'
import { addComplaintApi, getTenantComplaintListApi } from '../api/complaint'
import { usePagination } from '../composables/usePagination'
import { recommendHouseApi } from '../api/recommend'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const loading = ref(false)
const appointmentList = ref([])
const applicationList = ref([])
const contractList = ref([])
const orderList = ref([])
const repairList = ref([])
const complaintList = ref([])
const tenantSectionKeys = ['recommend', 'appointments', 'applications', 'contracts', 'orders', 'repairs', 'complaints']
const initialMenu = typeof route.query.menu === 'string' ? route.query.menu : 'recommend'
const activeMenu = ref(tenantSectionKeys.includes(initialMenu) ? initialMenu : 'recommend')
const repairDialogVisible = ref(false)
const complaintDialogVisible = ref(false)

const repairForm = ref({ houseId: '', content: '' })
const complaintForm = ref({ targetId: '', content: '' })

const recommendQuery = ref('')
const recommendModelType = ref('RULE')
const recommendTopK = ref(5)
const recommendLoading = ref(false)
const recommendList = ref([])

const recommendModelOptions = [
  { label: '规则推荐模型', value: 'RULE' },
  { label: 'TF-IDF文本相似度模型', value: 'TFIDF' },
  { label: '语义向量推荐模型', value: 'EMBEDDING' },
]

const currentRecommendModelLabel = computed(() => {
  return (
    recommendModelOptions.find((item) => item.value === recommendModelType.value)?.label ||
    '规则推荐模型'
  )
})

const paymentDialogVisible = ref(false)
const paymentLoading = ref(false)
const paymentSubmitting = ref(false)
const currentPayOrder = ref(null)
const selectedPayType = ref('ALIPAY')
const paymentPreview = ref(null)

const menus = [
  { key: 'agent', label: '智能租房助手' },
  { key: 'recommend', label: '智能找房' },
  { key: 'appointments', label: '我的预约' },
  { key: 'applications', label: '我的申请' },
  { key: 'contracts', label: '我的合同' },
  { key: 'orders', label: '我的订单' },
  { key: 'repairs', label: '我的报修' },
  { key: 'complaints', label: '我的投诉' },
]

const handleMenuClick = (key) => {
  if (key === 'agent') {
    router.push({ name: 'tenantAgent' })
    return
  }
  activeMenu.value = key
}

const unpaidOrderCount = computed(() => orderList.value.filter((item) => item.payStatus === 0).length)

const repairHouseOptions = computed(() => {
  const seen = new Set()
  return contractList.value
    .filter((item) => item.status === 1 && item.houseId && !seen.has(item.houseId) && (seen.add(item.houseId) || true))
    .map((item) => ({
      houseId: item.houseId,
      label: `${item.houseTitle || `房源 ${item.houseId}`} · 合同 ${item.id} · ${item.startDate} 至 ${item.endDate}`,
    }))
})

const complaintTargetOptions = computed(() => {
  const seen = new Set()
  return contractList.value
    .filter((item) => item.landlordId && !seen.has(item.landlordId) && (seen.add(item.landlordId) || true))
    .map((item) => ({
      value: item.landlordId,
      label: `${item.landlordName || `房东 ${item.landlordId}`}（合同 ${item.id}）`,
    }))
})

const { currentPage: appointmentPage, pageSize: appointmentPageSize, total: appointmentTotal, pagedList: pagedAppointmentList } = usePagination(appointmentList, 5)
const { currentPage: applicationPage, pageSize: applicationPageSize, total: applicationTotal, pagedList: pagedApplicationList } = usePagination(applicationList, 5)
const { currentPage: contractPage, pageSize: contractPageSize, total: contractTotal, pagedList: pagedContractList } = usePagination(contractList, 5)
const { currentPage: orderPage, pageSize: orderPageSize, total: orderTotal, pagedList: pagedOrderList } = usePagination(orderList, 5)
const { currentPage: repairPage, pageSize: repairPageSize, total: repairTotal, pagedList: pagedRepairList } = usePagination(repairList, 5)
const { currentPage: complaintPage, pageSize: complaintPageSize, total: complaintTotal, pagedList: pagedComplaintList } = usePagination(complaintList, 5)

const loadData = async () => {
  if (!userInfo.value?.id) return

  loading.value = true
  try {
    const [appointmentRes, applicationRes, contractRes, orderRes, repairRes, complaintRes] = await Promise.all([
      getTenantAppointmentListApi(userInfo.value.id),
      getTenantApplicationListApi(userInfo.value.id),
      getTenantContractListApi(userInfo.value.id),
      getTenantOrderListApi(userInfo.value.id),
      getTenantRepairListApi(userInfo.value.id),
      getTenantComplaintListApi(userInfo.value.id),
    ])

    appointmentList.value = appointmentRes.data || []
    applicationList.value = applicationRes.data || []
    contractList.value = contractRes.data || []
    orderList.value = orderRes.data || []
    repairList.value = repairRes.data || []
    complaintList.value = complaintRes.data || []
  } finally {
    loading.value = false
  }
}

const handleRecommend = async () => {
  if (!recommendQuery.value.trim()) {
    ElMessage.warning('请输入租房需求')
    return
  }

  recommendLoading.value = true
  try {
    const res = await recommendHouseApi({
      query: recommendQuery.value.trim(),
      modelType: recommendModelType.value,
      topK: recommendTopK.value,
    })

    recommendList.value = res.data || []

    if (recommendList.value.length === 0) {
      ElMessage.warning('暂无符合条件的推荐房源')
    } else {
      ElMessage.success('智能推荐完成')
    }
  } finally {
    recommendLoading.value = false
  }
}

const clearRecommend = () => {
  recommendQuery.value = ''
  recommendList.value = []
}

const goHouseDetail = (houseId) => {
  router.push(`/house/${houseId}`)
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

const handleAddRepair = async () => {
  if (!repairForm.value.houseId || !repairForm.value.content) {
    ElMessage.warning('请选择房源并填写报修内容')
    return
  }

  await addRepairApi({
    houseId: Number(repairForm.value.houseId),
    content: repairForm.value.content,
  })

  ElMessage.success('报修提交成功')
  repairDialogVisible.value = false
  repairForm.value = { houseId: '', content: '' }
  await loadData()
}

const handleAddComplaint = async () => {
  if (!complaintForm.value.content) {
    ElMessage.warning('请输入投诉内容')
    return
  }

  await addComplaintApi({
    targetId: complaintForm.value.targetId || null,
    content: complaintForm.value.content,
  })

  ElMessage.success('投诉提交成功')
  complaintDialogVisible.value = false
  complaintForm.value = { targetId: '', content: '' }
  await loadData()
}

const formatPayType = (payType) => {
  if (payType === 'ALIPAY') return '支付宝'
  if (payType === 'WECHAT') return '微信支付'
  return payType || '--'
}

const openPayDialog = async (row) => {
  currentPayOrder.value = row
  selectedPayType.value = 'ALIPAY'
  paymentPreview.value = null
  paymentDialogVisible.value = true
  await loadPaymentPreview()
}

const loadPaymentPreview = async () => {
  if (!currentPayOrder.value?.id) return
  paymentLoading.value = true
  try {
    const res = await startOrderPaymentApi(currentPayOrder.value.id, { payType: selectedPayType.value })
    paymentPreview.value = res.data
  } finally {
    paymentLoading.value = false
  }
}

const openMockCashier = () => {
  const popup = window.open('', '_blank', 'width=420,height=720')
  if (popup) {
    popup.document.write(`<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><title>支付宝收银台</title><style>body{font-family:Arial,Helvetica,sans-serif;background:#f5f7fa;padding:24px}.card{max-width:360px;margin:40px auto;background:#fff;border-radius:20px;padding:28px;box-shadow:0 12px 30px rgba(0,0,0,.08);text-align:center}.logo{font-size:24px;font-weight:700;color:#1677ff;margin-bottom:16px}.amount{font-size:28px;font-weight:700;color:#303133;margin:20px 0}.tip{color:#606266;line-height:1.8}.badge{display:inline-block;padding:6px 12px;border-radius:999px;background:#ecf5ff;color:#1677ff;margin-bottom:10px}</style></head><body><div class="card"><div class="badge">演示模式</div><div class="logo">支付宝收银台</div><div>订单号：${currentPayOrder.value?.id || '--'}</div><div class="amount">¥ ${currentPayOrder.value?.amount || '--'}</div><div class="tip">当前页面用于毕设演示。关闭此窗口后，回到系统点击“模拟支付成功”即可完成订单。</div></div></body></html>`)
    popup.document.close()
  }
  ElMessage.success('已打开支付宝收银台演示界面，请继续确认支付')
}

const confirmPayment = async () => {
  if (!currentPayOrder.value?.id) return
  paymentSubmitting.value = true
  try {
    await payOrderApi(currentPayOrder.value.id, { payType: selectedPayType.value })
    ElMessage.success(`${formatPayType(selectedPayType.value)}支付成功`)
    paymentDialogVisible.value = false
    currentPayOrder.value = null
    paymentPreview.value = null
    await loadData()
  } finally {
    paymentSubmitting.value = false
  }
}

const handleCancelOrder = async (row) => {
  await ElMessageBox.confirm('确定取消这笔订单吗？取消后房源会释放，需重新申请。', '提示', { type: 'warning' })
  await cancelOrderApi(row.id)
  ElMessage.success('订单已取消')
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
  loadData()
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
  font-size: 16px;
  color: #666;
  margin-bottom: 12px;
}
.summary-value {
  font-size: 32px;
  font-weight: 700;
  color: #409eff;
}
.summary-value.warning {
  color: #e6a23c;
}
.summary-value.primary {
  color: #303133;
}
.card-header {
  font-size: 18px;
  font-weight: 700;
}
.card-header-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.section-card {
  border-radius: 18px;
}
.payment-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.payment-order-box {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  line-height: 1.9;
}
.payment-amount {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}
.payment-channel-title {
  font-weight: 700;
}
.payment-preview {
  min-height: 260px;
  border-radius: 18px;
  border: 1px solid #ebeef5;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
}
.wechat-pay-box,
.cashier-box {
  text-align: center;
}
.wechat-pay-qrcode {
  width: 220px;
  height: 220px;
  object-fit: contain;
}
.wechat-pay-tip,
.cashier-tip,
.cashier-desc {
  color: #606266;
  margin-top: 10px;
  line-height: 1.8;
}
.cashier-title {
  font-size: 24px;
  font-weight: 700;
  color: #1677ff;
}
.recommend-card {
  margin-bottom: 20px;
}

.recommend-intro {
  margin-bottom: 16px;
  color: #606266;
  line-height: 1.8;
}

.recommend-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.recommend-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.recommend-model {
  width: 220px;
}

.recommend-topk {
  width: 140px;
}
@media (max-width: 768px) {
  .summary-row :deep(.el-col) {
    margin-bottom: 16px;
  }
}
</style>
