<template>
  <div class="detail-page" v-loading="loading">
    <div class="detail-banner">
      <div>
        <div class="detail-breadcrumb">房源中心 / 详情页</div>
        <h2>{{ detail?.title || '房源详情' }}</h2>
        <p>{{ detail?.city }} · {{ detail?.area }} · {{ detail?.address }}</p>
      </div>

      <div class="banner-actions">
        <el-button @click="$router.push('/houses')">返回房源列表</el-button>
        <el-button v-if="isTenant && detail?.status === 1" type="primary" @click="appointmentDialogVisible = true">预约看房</el-button>
        <el-button v-if="isTenant && detail?.status === 1" type="success" @click="applicationDialogVisible = true">提交申请</el-button>
      </div>
    </div>

    <div class="detail-content" v-if="detail">
      <div class="left-panel">
        <el-card class="info-card image-card">
          <template #header><div class="side-title">房源图片</div></template>
          <el-carousel v-if="imageList.length" height="360px" trigger="click" indicator-position="outside">
            <el-carousel-item v-for="(item, index) in imageList" :key="`${item}-${index}`">
              <el-image :src="item" fit="cover" class="carousel-image" :preview-src-list="imageList" preview-teleported />
            </el-carousel-item>
          </el-carousel>
          <el-empty v-else description="暂无房源图片" />
        </el-card>

        <el-card class="info-card">
          <div class="price-box">
            <div class="price-main">¥ {{ detail.rentPrice }}</div>
            <div class="price-sub">押金：¥ {{ detail.deposit }}</div>
          </div>

          <div class="tags-row">
            <el-tag size="large">{{ detail.houseType }}</el-tag>
            <el-tag type="success" size="large">{{ detail.city }}</el-tag>
            <el-tag type="warning" size="large">{{ detail.area }}</el-tag>
            <el-tag v-if="hasCoordinates" type="info" size="large">支持导航</el-tag>
          </div>

          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="城市">{{ detail.city }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ detail.area }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ detail.address }}</el-descriptions-item>
            <el-descriptions-item label="户型">{{ detail.houseType }}</el-descriptions-item>
            <el-descriptions-item label="面积">{{ detail.square }}</el-descriptions-item>
            <el-descriptions-item label="楼层">{{ detail.floor }}</el-descriptions-item>
            <el-descriptions-item label="发布者ID">{{ detail.publisherId }}</el-descriptions-item>
            <el-descriptions-item label="房源状态">{{ formatHouseStatus(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="经度">{{ detail.longitude ?? '未配置' }}</el-descriptions-item>
            <el-descriptions-item label="纬度">{{ detail.latitude ?? '未配置' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ detail.description }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>

      <div class="right-panel">
        <el-card class="tips-card">
          <template #header><div class="side-title">位置与导航</div></template>
          <div class="location-card">
            <div class="location-label">目的地地址</div>
            <div class="location-value">{{ fullAddress }}</div>

            <div class="coord-grid">
              <div>
                <div class="coord-label">经度</div>
                <div class="coord-value">{{ detail.longitude ?? '--' }}</div>
              </div>
              <div>
                <div class="coord-label">纬度</div>
                <div class="coord-value">{{ detail.latitude ?? '--' }}</div>
              </div>
            </div>

            <el-alert
              :title="hasCoordinates ? '已配置房屋坐标，可直接打开地图导航。' : '当前房源暂未配置坐标，可先复制地址后再导航。'"
              type="info"
              :closable="false"
              show-icon
            />

            <div class="side-actions">
              <el-button type="primary" @click="openAmapNavigation">打开高德导航</el-button>
              <el-button @click="copyAddress">复制地址</el-button>
              <el-button @click="$router.push('/houses')">继续看房</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="tips-card">
          <template #header><div class="side-title">租房提示</div></template>
          <ul class="tips-list">
            <li>先查看房源图片与详情，确认价格、地址和户型信息。</li>
            <li>感兴趣可先预约看房，再提交租房申请。</li>
            <li>申请通过后可在租客中心查看合同、订单、报修和投诉。</li>
            <li>出发前可点击“打开高德导航”直接导航到房屋位置。</li>
          </ul>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="appointmentDialogVisible" title="预约看房" width="500px">
      <el-form :model="appointmentForm" label-width="100px">
        <el-form-item label="预约时间">
          <el-date-picker
            v-model="appointmentForm.appointmentTime"
            type="datetime"
            placeholder="请选择预约时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="appointmentForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAppointment">提交预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applicationDialogVisible" title="提交租房申请" width="500px">
      <el-form :model="applicationForm" label-width="100px">
        <el-form-item label="备注">
          <el-input v-model="applicationForm.remark" type="textarea" placeholder="请输入申请备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleApplication">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getAdminHouseDetailApi, getHouseDetailApi, getLandlordHouseDetailApi } from '../api/house'
import { addAppointmentApi } from '../api/appointment'
import { addRentalApplicationApi } from '../api/application'
import { parseImageUrls } from '../utils/house'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detail = ref(null)

const appointmentDialogVisible = ref(false)
const applicationDialogVisible = ref(false)

const appointmentForm = reactive({
  appointmentTime: '',
  remark: '',
})

const applicationForm = reactive({ remark: '' })

const userInfo = computed(() => userStore.userInfo)
const isTenant = computed(() => userInfo.value?.roleCode === 'TENANT')
const imageList = computed(() => parseImageUrls(detail.value?.imageUrls))
const detailScope = computed(() => route.query.scope || '')
const hasCoordinates = computed(() => detail.value?.longitude !== null && detail.value?.longitude !== undefined
  && detail.value?.latitude !== null && detail.value?.latitude !== undefined)
const fullAddress = computed(() => [detail.value?.city, detail.value?.area, detail.value?.address].filter(Boolean).join(' '))

const formatHouseStatus = (status) => {
  if (status === 0) return '已下架'
  if (status === 1) return '已上架'
  if (status === 2) return '待支付'
  if (status === 3) return '已出租'
  return '--'
}

const loadDetail = async () => {
  loading.value = true
  try {
    let res
    if (userInfo.value?.roleCode === 'LANDLORD' && detailScope.value === 'landlord') {
      res = await getLandlordHouseDetailApi(route.params.id)
    } else if (userInfo.value?.roleCode === 'ADMIN' && detailScope.value === 'admin') {
      res = await getAdminHouseDetailApi(route.params.id)
    } else {
      res = await getHouseDetailApi(route.params.id)
    }
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

const buildAmapUrl = () => {
  if (!detail.value) return 'https://www.amap.com/'
  if (hasCoordinates.value) {
    const name = encodeURIComponent(detail.value.title || '房屋位置')
    return `https://uri.amap.com/navigation?to=${detail.value.longitude},${detail.value.latitude},${name}&mode=car&coordinate=gaode&src=house-rental-web&callnative=1`
  }
  return `https://www.amap.com/search?query=${encodeURIComponent(fullAddress.value)}`
}

const openAmapNavigation = () => {
  window.open(buildAmapUrl(), '_blank')
}

const copyAddress = async () => {
  await navigator.clipboard.writeText(fullAddress.value)
  ElMessage.success('地址已复制')
}

const handleAppointment = async () => {
  if (!userInfo.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!appointmentForm.appointmentTime) {
    ElMessage.warning('请选择预约时间')
    return
  }
  await addAppointmentApi({
    houseId: detail.value.id,
    appointmentTime: appointmentForm.appointmentTime,
    remark: appointmentForm.remark,
  })
  ElMessage.success('预约成功')
  appointmentDialogVisible.value = false
  appointmentForm.appointmentTime = ''
  appointmentForm.remark = ''
}

const handleApplication = async () => {
  if (!userInfo.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await addRentalApplicationApi({
    houseId: detail.value.id,
    remark: applicationForm.remark,
  })
  ElMessage.success('申请提交成功')
  applicationDialogVisible.value = false
  applicationForm.remark = ''
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fa;
}

.detail-banner {
  max-width: 1180px;
  margin: 0 auto 20px;
  padding: 26px 28px;
  border-radius: 22px;
  background: linear-gradient(135deg, #ffffff 0%, #eef5ff 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.detail-breadcrumb {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.detail-banner h2 {
  margin: 0 0 8px;
  font-size: 34px;
}

.detail-banner p {
  margin: 0;
  color: #606266;
}

.banner-actions {
  display: flex;
  gap: 12px;
}

.detail-content {
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 20px;
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card,
.tips-card {
  border-radius: 20px;
}

.image-card {
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 360px;
}

.price-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.price-main {
  color: #f56c6c;
  font-size: 34px;
  font-weight: 700;
}

.price-sub {
  color: #606266;
}

.tags-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.detail-desc {
  margin-top: 8px;
}

.side-title {
  font-size: 18px;
  font-weight: 700;
}

.location-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.location-label,
.coord-label {
  color: #909399;
  font-size: 13px;
}

.location-value,
.coord-value {
  margin-top: 6px;
  color: #303133;
  line-height: 1.7;
}

.coord-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.side-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}

@media (max-width: 960px) {
  .detail-content {
    grid-template-columns: 1fr;
  }

  .detail-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
