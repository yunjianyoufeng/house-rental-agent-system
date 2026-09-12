<template>
  <DashboardShell
    title="管理员首页"
    :role="userInfo?.roleCode || ''"
    :menus="menus"
    :active-menu="activeMenu"
    @menu-click="activeMenu = $event"
    @logout="logout"
  >
    <template #header-actions>
      <el-button v-if="activeMenu === 'notice'" type="primary" @click="openNoticeCreateDialog">
        发布公告
      </el-button>
    </template>

    <template v-if="activeMenu === 'overview'">
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="6" v-for="item in cards" :key="item.label">
          <el-card class="stat-card">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="section-card chart-card">
        <template #header>
          <div class="card-header">运营概览图表</div>
        </template>
        <div ref="overviewChartRef" class="overview-chart" />
      </el-card>
    </template>

    <template v-if="activeMenu === 'users'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">用户管理</div>
        </template>

        <el-table :data="pagedUserList" border v-loading="loading">
          <el-table-column prop="id" label="用户ID" width="100" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="realName" label="姓名" width="140" />
          <el-table-column prop="roleCode" label="角色" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.roleCode === 'ADMIN'">管理员</el-tag>
              <el-tag v-else-if="scope.row.roleCode === 'LANDLORD'" type="warning">出租者</el-tag>
              <el-tag v-else type="success">租客</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="150" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 1"
                type="danger"
                link
                @click="handleToggleUserStatus(scope.row, 0)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                type="success"
                link
                @click="handleToggleUserStatus(scope.row, 1)"
              >
                启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="userPage"
            v-model:page-size="userPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="userTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'audit'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">房源审核</div>
        </template>

        <el-table :data="pagedAuditHouseList" border v-loading="auditLoading">
          <el-table-column label="封面" width="100">
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
          <el-table-column prop="city" label="城市" width="120" />
          <el-table-column prop="area" label="区域" width="120" />
          <el-table-column prop="address" label="地址" min-width="200" />
          <el-table-column prop="rentPrice" label="租金" width="120" />
          <el-table-column prop="publisherName" label="发布者" width="140" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button type="success" link @click="handleApproveHouse(scope.row.id)">同意</el-button>
              <el-button type="danger" link @click="handleRejectHouse(scope.row.id)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="auditHousePage"
            v-model:page-size="auditHousePageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="auditHouseTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'applications'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">申请管理</div>
        </template>

        <el-table :data="pagedApplicationList" border v-loading="loading">
          <el-table-column prop="id" label="申请ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="tenantName" label="租客名称" width="140" />
          <el-table-column prop="landlordName" label="出租者名称" width="140" />
          <el-table-column prop="remark" label="备注" min-width="220" />
          <el-table-column prop="status" label="申请状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已拒绝</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="applicationPage"
            v-model:page-size="applicationPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="applicationTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'contracts'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">合同管理</div>
        </template>

        <el-table :data="pagedContractList" border v-loading="loading">
          <el-table-column prop="id" label="合同ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="tenantName" label="租客名称" width="140" />
          <el-table-column prop="landlordName" label="出租者名称" width="140" />
          <el-table-column prop="applicationId" label="申请ID" width="100" />
          <el-table-column prop="startDate" label="开始日期" width="140" />
          <el-table-column prop="endDate" label="结束日期" width="140" />
          <el-table-column prop="monthlyRent" label="月租金" width="120" />
          <el-table-column prop="deposit" label="押金" width="120" />
          <el-table-column label="合同附件" min-width="190">
            <template #default="scope">
              <div class="table-inline-actions">
                <el-button
                  v-if="scope.row.contractUrl"
                  type="primary"
                  link
                  @click="openFile(scope.row.id)"
                >
                  查看附件
                </el-button>
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                  @change="(uploadFile) => handleAdminContractUpload(uploadFile, scope.row)"
                >
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
          <el-table-column label="操作" width="120">
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

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="contractPage"
            v-model:page-size="contractPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="contractTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'orders'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">订单管理</div>
        </template>

        <el-table :data="pagedOrderList" border v-loading="loading">
          <el-table-column prop="id" label="订单ID" width="100" />
          <el-table-column prop="contractId" label="合同ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="tenantName" label="租客名称" width="140" />
          <el-table-column prop="landlordName" label="出租者名称" width="140" />
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

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="orderPage"
            v-model:page-size="orderPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="orderTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'repairs'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">报修管理</div>
        </template>

        <el-table :data="pagedRepairList" border v-loading="loading">
          <el-table-column prop="id" label="报修ID" width="100" />
          <el-table-column prop="houseTitle" label="房源名称" min-width="180" />
          <el-table-column prop="tenantName" label="租客名称" width="140" />
          <el-table-column prop="landlordName" label="出租者名称" width="140" />
          <el-table-column prop="content" label="报修内容" min-width="220" />
          <el-table-column prop="status" label="处理状态" width="120">
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
          <el-pagination
            v-model:current-page="repairPage"
            v-model:page-size="repairPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="repairTotal"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeMenu === 'complaints'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">投诉管理</div>
        </template>

        <el-table :data="pagedComplaintList" border v-loading="loading">
          <el-table-column prop="id" label="投诉ID" width="100" />
          <el-table-column prop="userName" label="投诉人" width="140" />
          <el-table-column prop="targetName" label="投诉对象" width="140">
            <template #default="scope">
              <span>{{ scope.row.targetName || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="投诉内容" min-width="220" />
          <el-table-column prop="status" label="状态" width="110">
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
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 0"
                type="primary"
                link
                @click="openComplaintDialog(scope.row)"
              >
                处理
              </el-button>
              <span v-else>--</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="complaintPage"
            v-model:page-size="complaintPageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="complaintTotal"
          />
        </div>
      </el-card>

      <el-dialog v-model="complaintDialogVisible" title="处理投诉" width="560px">
        <el-form label-width="90px">
          <el-form-item label="投诉ID">
            <el-input :model-value="currentComplaint?.id || ''" disabled />
          </el-form-item>
          <el-form-item label="处理结果">
            <el-input
              v-model="complaintProcessForm.result"
              type="textarea"
              :rows="4"
              placeholder="请输入处理结果"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="complaintDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleProcessComplaint">提交处理</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-if="activeMenu === 'recommendEval'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header card-header-between">
            <span>推荐模型评价</span>
            <el-tag type="primary">智能房源推荐</el-tag>
          </div>
        </template>

        <div class="eval-intro">
          本页面用于根据推荐实验查询表和人工标注表，对推荐模型的 Top-K 推荐结果进行评价，
          主要统计 Precision@K、Recall@K、F1@K、NDCG@K 和平均响应时间。
        </div>

        <div class="eval-form">
          <el-select v-model="recommendEvalForm.modelType" class="eval-select" placeholder="模型类型">
            <el-option label="规则推荐 RULE" value="RULE" />
            <el-option label="TF-IDF 推荐 TFIDF" value="TFIDF" />
            <el-option label="语义向量推荐 EMBEDDING" value="EMBEDDING" />
          </el-select>

          <el-select v-model="recommendEvalForm.topK" class="eval-select" placeholder="TopK">
            <el-option label="Top 3" :value="3" />
            <el-option label="Top 5" :value="5" />
            <el-option label="Top 10" :value="10" />
          </el-select>

          <el-select
            v-model="recommendEvalForm.sceneType"
            class="eval-select"
            placeholder="场景类型"
            clearable
          >
            <el-option label="预算学生" value="预算学生" />
            <el-option label="交通" value="交通" />
            <el-option label="装修" value="装修" />
            <el-option label="学习环境" value="学习环境" />
            <el-option label="家庭居住" value="家庭居住" />
          </el-select>

          <el-button type="primary" :loading="recommendEvalLoading" @click="loadRecommendEval">
            计算评价指标
          </el-button>

          <el-button @click="resetRecommendEval">
            重置
          </el-button>
        </div>
      </el-card>

      <el-row v-if="recommendEvalResult" :gutter="20" v-loading="recommendEvalLoading">
        <el-col :span="4">
          <el-card class="stat-card">
            <div class="stat-label">查询数量</div>
            <div class="stat-value">{{ recommendEvalResult.queryCount }}</div>
          </el-card>
        </el-col>

        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-label">Precision@{{ recommendEvalResult.topK }}</div>
            <div class="stat-value">{{ recommendEvalResult.avgPrecision }}</div>
          </el-card>
        </el-col>

        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-label">Recall@{{ recommendEvalResult.topK }}</div>
            <div class="stat-value">{{ recommendEvalResult.avgRecall }}</div>
          </el-card>
        </el-col>

        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-label">F1@{{ recommendEvalResult.topK }}</div>
            <div class="stat-value">{{ recommendEvalResult.avgF1 }}</div>
          </el-card>
        </el-col>

        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-label">NDCG@{{ recommendEvalResult.topK }}</div>
            <div class="stat-value">{{ recommendEvalResult.avgNdcg }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="recommendEvalResult" class="section-card">
        <template #header>
          <div class="card-header card-header-between">
            <span>评价结果明细</span>
            <el-tag type="success">
              平均响应时间：{{ recommendEvalResult.avgResponseTimeMs }} ms
            </el-tag>
          </div>
        </template>

        <el-table :data="recommendEvalResult.details || []" border v-loading="recommendEvalLoading">
          <el-table-column prop="queryId" label="查询ID" width="90" />
          <el-table-column prop="queryText" label="测试查询语句" min-width="260" />
          <el-table-column prop="sceneType" label="场景类型" width="120" />

          <el-table-column label="推荐房源ID" min-width="160">
            <template #default="scope">
              {{ (scope.row.recommendedHouseIds || []).join(', ') || '--' }}
            </template>
          </el-table-column>

          <el-table-column label="标注房源ID" min-width="160">
            <template #default="scope">
              {{ (scope.row.relevantHouseIds || []).join(', ') || '--' }}
            </template>
          </el-table-column>

          <el-table-column prop="hitCount" label="命中数" width="90" />
          <el-table-column prop="precision" label="Precision" width="110" />
          <el-table-column prop="recall" label="Recall" width="100" />
          <el-table-column prop="f1" label="F1" width="100" />
          <el-table-column prop="ndcg" label="NDCG" width="100" />
          <el-table-column prop="responseTimeMs" label="响应时间/ms" width="130" />
        </el-table>
      </el-card>

      <el-empty
        v-if="!recommendEvalLoading && !recommendEvalResult"
        description="暂无模型评价结果，请点击计算评价指标"
      />
    </template>

    <template v-if="activeMenu === 'notice'">
      <el-card class="section-card">
        <template #header>
          <div class="card-header">公告管理</div>
        </template>

        <el-table :data="pagedNoticeList" border v-loading="noticeLoading">
          <el-table-column prop="id" label="公告ID" width="100" />
          <el-table-column prop="title" label="公告标题" min-width="180" />
          <el-table-column prop="content" label="公告内容" min-width="260" />
          <el-table-column prop="createTime" label="创建时间" min-width="170" />
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button type="primary" link @click="openNoticeEditDialog(scope.row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="noticePage"
            v-model:page-size="noticePageSize"
            :page-sizes="[5, 10, 20]"
            background
            layout="total, sizes, prev, pager, next"
            :total="noticeTotal"
          />
        </div>
      </el-card>
    </template>

    <el-dialog
      v-model="noticeDialogVisible"
      :title="noticeDialogMode === 'create' ? '发布公告' : '编辑公告'"
      width="520px"
    >
      <el-form :model="noticeForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="noticeForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noticeForm.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitNotice">
          {{ noticeDialogMode === 'create' ? '发布' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </DashboardShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { logoutApi } from '../api/auth'
import DashboardShell from '../components/DashboardShell.vue'
import { getStatisticsOverviewApi } from '../api/statistics'
import { addNoticeApi, deleteNoticeApi, getNoticeListApi, updateNoticeApi } from '../api/notice'
import { approveAuditHouseApi, getAuditHouseListApi, rejectAuditHouseApi } from '../api/house'
import { downloadContractFileApi, finishContractAdminApi, getAdminContractListApi, updateAdminContractFileApi } from '../api/contract'
import { getAdminOrderListApi } from '../api/order'
import { getAdminRepairListApi } from '../api/repair'
import { getAdminApplicationListApi } from '../api/application'
import { getAdminComplaintListApi, processComplaintApi } from '../api/complaint'
import { getAdminUserListApi, updateUserStatusApi } from '../api/user'
import { uploadAdminContractFileApi } from '../api/upload'
import { evaluateRecommendApi } from '../api/recommend'
import { getFirstHouseImage, parseImageUrls } from '../utils/house'
import { usePagination } from '../composables/usePagination'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const loading = ref(false)
const noticeLoading = ref(false)
const auditLoading = ref(false)
const activeMenu = ref('overview')
const noticeDialogVisible = ref(false)
const noticeDialogMode = ref('create')
const editingNoticeId = ref(null)

const menus = [
  { key: 'overview', label: '数据总览' },
  { key: 'users', label: '用户管理' },
  { key: 'audit', label: '房源审核' },
  { key: 'applications', label: '申请管理' },
  { key: 'contracts', label: '合同管理' },
  { key: 'orders', label: '订单管理' },
  { key: 'repairs', label: '报修管理' },
  { key: 'complaints', label: '投诉管理' },
  { key: 'recommendEval', label: '模型评价' },
  { key: 'notice', label: '公告管理' },
]

const overview = ref({
  userCount: 0,
  houseCount: 0,
  pendingAuditHouseCount: 0,
  orderCount: 0,
  paidOrderCount: 0,
  repairCount: 0,
  complaintCount: 0,
  noticeCount: 0,
})

const noticeList = ref([])
const userList = ref([])
const auditHouseList = ref([])
const applicationList = ref([])
const contractList = ref([])
const orderList = ref([])
const repairList = ref([])
const complaintList = ref([])

const { currentPage: userPage, pageSize: userPageSize, total: userTotal, pagedList: pagedUserList } = usePagination(userList, 5)
const { currentPage: auditHousePage, pageSize: auditHousePageSize, total: auditHouseTotal, pagedList: pagedAuditHouseList } = usePagination(auditHouseList, 5)
const { currentPage: applicationPage, pageSize: applicationPageSize, total: applicationTotal, pagedList: pagedApplicationList } = usePagination(applicationList, 5)
const { currentPage: contractPage, pageSize: contractPageSize, total: contractTotal, pagedList: pagedContractList } = usePagination(contractList, 5)
const { currentPage: orderPage, pageSize: orderPageSize, total: orderTotal, pagedList: pagedOrderList } = usePagination(orderList, 5)
const { currentPage: repairPage, pageSize: repairPageSize, total: repairTotal, pagedList: pagedRepairList } = usePagination(repairList, 5)
const { currentPage: complaintPage, pageSize: complaintPageSize, total: complaintTotal, pagedList: pagedComplaintList } = usePagination(complaintList, 5)
const { currentPage: noticePage, pageSize: noticePageSize, total: noticeTotal, pagedList: pagedNoticeList } = usePagination(noticeList, 5)

const noticeForm = reactive({ title: '', content: '' })
const complaintDialogVisible = ref(false)
const currentComplaint = ref(null)
const complaintProcessForm = reactive({ result: '' })

const recommendEvalLoading = ref(false)
const recommendEvalResult = ref(null)
const recommendEvalForm = reactive({
  modelType: 'RULE',
  topK: 5,
  sceneType: '',
})

const cards = computed(() => [
  { label: '用户总数', value: overview.value.userCount },
  { label: '房源总数', value: overview.value.houseCount },
  { label: '待审核房源', value: overview.value.pendingAuditHouseCount },
  { label: '订单总数', value: overview.value.orderCount },
  { label: '已支付订单', value: overview.value.paidOrderCount },
  { label: '报修总数', value: overview.value.repairCount },
  { label: '投诉总数', value: overview.value.complaintCount },
  { label: '公告总数', value: overview.value.noticeCount },
])

const overviewChartRef = ref(null)
let overviewChart = null

const renderOverviewChart = async () => {
  if (activeMenu.value !== 'overview') return
  await nextTick()
  if (!overviewChartRef.value) return

  if (!overviewChart) {
    overviewChart = echarts.init(overviewChartRef.value)
  }

  overviewChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['用户', '房源', '待审', '订单', '已支付', '报修', '投诉', '公告'],
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [
        overview.value.userCount,
        overview.value.houseCount,
        overview.value.pendingAuditHouseCount,
        overview.value.orderCount,
        overview.value.paidOrderCount,
        overview.value.repairCount,
        overview.value.complaintCount,
        overview.value.noticeCount,
      ],
      barMaxWidth: 40,
    }],
  })
}

const loadOverview = async () => {
  loading.value = true
  try {
    const res = await getStatisticsOverviewApi()
    overview.value = res.data
  } finally {
    loading.value = false
  }
}

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await getAdminUserListApi()
    userList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadNoticeList = async () => {
  noticeLoading.value = true
  try {
    const res = await getNoticeListApi()
    noticeList.value = res.data || []
  } finally {
    noticeLoading.value = false
  }
}

const loadAuditHouseList = async () => {
  auditLoading.value = true
  try {
    const res = await getAuditHouseListApi()
    auditHouseList.value = res.data || []
  } finally {
    auditLoading.value = false
  }
}

const loadContractList = async () => {
  loading.value = true
  try {
    const res = await getAdminContractListApi()
    contractList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadOrderList = async () => {
  loading.value = true
  try {
    const res = await getAdminOrderListApi()
    orderList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRepairList = async () => {
  loading.value = true
  try {
    const res = await getAdminRepairListApi()
    repairList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadApplicationList = async () => {
  loading.value = true
  try {
    const res = await getAdminApplicationListApi()
    applicationList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadComplaintList = async () => {
  loading.value = true
  try {
    const res = await getAdminComplaintListApi()
    complaintList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRecommendEval = async () => {
  recommendEvalLoading.value = true
  try {
    const payload = {
      modelType: recommendEvalForm.modelType,
      topK: recommendEvalForm.topK,
    }

    if (recommendEvalForm.sceneType) {
      payload.sceneType = recommendEvalForm.sceneType
    }

    const res = await evaluateRecommendApi(payload)
    recommendEvalResult.value = res.data
    ElMessage.success('模型评价计算完成')
  } catch (error) {
    console.error(error)
    ElMessage.error('模型评价计算失败，请确认后端和 Python 语义推荐服务是否正常运行')
  } finally {
    recommendEvalLoading.value = false
  }
}

const resetRecommendEval = () => {
  recommendEvalForm.modelType = 'RULE'
  recommendEvalForm.topK = 5
  recommendEvalForm.sceneType = ''
  recommendEvalResult.value = null
}

watch(activeMenu, () => {
  renderOverviewChart()

  if (activeMenu.value === 'recommendEval' && !recommendEvalResult.value) {
    loadRecommendEval()
  }
})

watch(overview, () => {
  renderOverviewChart()
}, { deep: true })

const openFile = async (contractId) => {
  if (!contractId) return
  try {
    const blob = await downloadContractFileApi(contractId)
    const fileUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = fileUrl
    link.download = blob.name || `合同附件-${contractId}`
    link.rel = 'noopener noreferrer'
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(fileUrl), 60000)
  } catch (error) {
    console.log('合同附件打开失败：', error)
  }
}

const handleAdminContractUpload = async (uploadFile, row) => {
  const realFile = uploadFile?.raw || uploadFile

  if (!realFile) {
    ElMessage.error('请选择要上传的文件')
    return
  }

  if (!row?.id) {
    ElMessage.error('合同信息不存在，无法上传附件')
    return
  }

  try {
    const uploadRes = await uploadAdminContractFileApi(realFile)
    const fileUrl = uploadRes?.data

    if (!fileUrl) {
      console.log('合同附件上传接口返回：', uploadRes)
      ElMessage.error('合同附件上传成功，但未获取到文件地址')
      return
    }

    await updateAdminContractFileApi(row.id, fileUrl)
    ElMessage.success('合同附件上传成功')
    await Promise.all([loadContractList(), loadOverview()])
  } catch (error) {
    console.error('合同附件上传失败：', error)
    ElMessage.error(error?.message || '合同附件上传失败')
  }
}

const openNoticeCreateDialog = () => {
  noticeDialogMode.value = 'create'
  editingNoticeId.value = null
  noticeForm.title = ''
  noticeForm.content = ''
  noticeDialogVisible.value = true
}

const openNoticeEditDialog = (row) => {
  noticeDialogMode.value = 'edit'
  editingNoticeId.value = row.id
  noticeForm.title = row.title
  noticeForm.content = row.content
  noticeDialogVisible.value = true
}

const handleSubmitNotice = async () => {
  if (!noticeForm.title || !noticeForm.content) {
    ElMessage.warning('请填写完整公告信息')
    return
  }

  if (noticeDialogMode.value === 'create') {
    await addNoticeApi({ title: noticeForm.title, content: noticeForm.content })
    ElMessage.success('公告发布成功')
  } else {
    await updateNoticeApi(editingNoticeId.value, { title: noticeForm.title, content: noticeForm.content })
    ElMessage.success('公告更新成功')
  }

  noticeDialogVisible.value = false
  await Promise.all([loadNoticeList(), loadOverview()])
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除这条公告吗？', '提示', { type: 'warning' })
  await deleteNoticeApi(id)
  ElMessage.success('公告删除成功')
  await Promise.all([loadNoticeList(), loadOverview()])
}

const handleToggleUserStatus = async (row, status) => {
  await ElMessageBox.confirm(`确定${status === 1 ? '启用' : '禁用'}用户“${row.username}”吗？`, '提示', { type: 'warning' })
  await updateUserStatusApi(row.id, status)
  ElMessage.success(status === 1 ? '用户已启用' : '用户已禁用')
  await Promise.all([loadUserList(), loadOverview()])
}

const handleApproveHouse = async (id) => {
  await approveAuditHouseApi(id)
  ElMessage.success('房源审核通过')
  await Promise.all([loadAuditHouseList(), loadOverview()])
}

const handleRejectHouse = async (id) => {
  await rejectAuditHouseApi(id)
  ElMessage.success('已拒绝该房源')
  await Promise.all([loadAuditHouseList(), loadOverview()])
}

const handleFinishContract = async (row) => {
  await ElMessageBox.confirm(`确定结束合同 ${row.id} 吗？结束后房源将回收为下架状态。`, '提示', { type: 'warning' })
  await finishContractAdminApi(row.id)
  ElMessage.success('合同已结束')
  await Promise.all([loadContractList(), loadOrderList(), loadOverview()])
}

const formatPayType = (payType) => {
  if (payType === 'ALIPAY') return '支付宝'
  if (payType === 'WECHAT') return '微信支付'
  return payType || '--'
}

const openComplaintDialog = (row) => {
  currentComplaint.value = row
  complaintProcessForm.result = row.result || ''
  complaintDialogVisible.value = true
}

const handleProcessComplaint = async () => {
  if (!currentComplaint.value?.id) return
  if (!complaintProcessForm.result) {
    ElMessage.warning('请输入处理结果')
    return
  }

  await processComplaintApi(currentComplaint.value.id, {
    status: 1,
    result: complaintProcessForm.result,
  })
  ElMessage.success('投诉处理成功')
  complaintDialogVisible.value = false
  currentComplaint.value = null
  complaintProcessForm.result = ''
  await Promise.all([loadComplaintList(), loadOverview()])
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

onMounted(async () => {
  await Promise.all([
    loadOverview(),
    loadUserList(),
    loadNoticeList(),
    loadAuditHouseList(),
    loadApplicationList(),
    loadContractList(),
    loadOrderList(),
    loadRepairList(),
    loadComplaintList(),
  ])
  renderOverviewChart()
  window.addEventListener('resize', renderOverviewChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderOverviewChart)
  if (overviewChart) {
    overviewChart.dispose()
    overviewChart = null
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 20px;
}

.stat-label {
  font-size: 16px;
  color: #666;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  color: #409eff;
}

.card-header {
  font-size: 18px;
  font-weight: 700;
}

.card-header-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-card {
  margin-top: 4px;
}

.overview-chart {
  width: 100%;
  height: 360px;
}

.table-cover {
  width: 60px;
  height: 60px;
  border-radius: 10px;
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

.table-inline-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.eval-intro {
  margin-bottom: 16px;
  color: #606266;
  line-height: 1.8;
}

.eval-form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.eval-select {
  width: 180px;
}
</style>
