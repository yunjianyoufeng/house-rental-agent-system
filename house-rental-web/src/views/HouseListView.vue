<template>
  <div class="house-page">
    <div class="banner">
      <div>
        <h2>房源列表</h2>
        <p>支持搜索、价格筛选、排序、图片预览、地图导航与智能房源推荐。</p>
      </div>

      <div class="banner-actions">
        <el-tag type="success" size="large">
          {{ isRecommendMode ? '推荐房源数' : '房源数' }}：{{ displayHouseList.length }}
        </el-tag>
        <el-button @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>

    <el-card class="section-card recommend-card">
      <div class="recommend-title-row">
        <div>
          <h3>智能找房</h3>
          <p>输入自然语言租房需求，系统会根据租金、位置、户型、交通、装修等条件推荐房源。</p>
        </div>
        <el-tag type="primary">{{ currentRecommendModelLabel }}</el-tag>
      </div>

      <div class="recommend-form">
        <el-input
          v-model="recommendQuery"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="例如：我想找精装公寓，交通方便，可以直接入住，适合一个人住"
        />

        <div class="recommend-actions">
          <el-select v-model="recommendModelType" placeholder="推荐模型" class="model-select">
            <el-option
              v-for="item in recommendModelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-select v-model="recommendTopK" placeholder="推荐数量" class="topk-select">
            <el-option label="推荐 3 套" :value="3" />
            <el-option label="推荐 5 套" :value="5" />
            <el-option label="推荐 8 套" :value="8" />
          </el-select>

          <el-button type="primary" :loading="recommendLoading" @click="handleRecommend">
            开始推荐
          </el-button>

          <el-button @click="clearRecommend">
            清空推荐
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="isRecommendMode"
        class="recommend-alert"
        type="success"
        show-icon
        :closable="false"
        title="当前正在展示智能推荐结果，如需查看全部房源，请点击“清空推荐”。"
      />
    </el-card>

    <el-card class="section-card filter-card">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="请输入标题/地址关键字" clearable class="filter-input" />
        <el-select v-model="cityFilter" placeholder="选择城市" clearable class="filter-select">
          <el-option v-for="item in cityOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="areaFilter" placeholder="选择区域" clearable class="filter-select">
          <el-option v-for="item in areaOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="houseTypeFilter" placeholder="选择户型" clearable class="filter-select">
          <el-option v-for="item in houseTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="priceFilter" placeholder="租金区间" clearable class="filter-select">
          <el-option label="3000 以下" value="lt3000" />
          <el-option label="3000 - 5000" value="3000-5000" />
          <el-option label="5000 - 8000" value="5000-8000" />
          <el-option label="8000 以上" value="gt8000" />
        </el-select>
        <el-select v-model="sortType" placeholder="排序方式" class="filter-select">
          <el-option label="最新发布" value="latest" />
          <el-option label="租金从低到高" value="priceAsc" />
          <el-option label="租金从高到低" value="priceDesc" />
        </el-select>
        <el-button @click="resetFilter">重置筛选</el-button>
      </div>
    </el-card>

    <div class="result-title">
      <div>
        <h3>{{ isRecommendMode ? '智能推荐结果' : '全部房源' }}</h3>
        <p v-if="isRecommendMode">推荐结果按照当前选择的推荐模型得分从高到低排序。</p>
        <p v-else>以下为当前筛选条件下的房源列表。</p>
      </div>
    </div>

    <div class="house-grid" v-loading="loading || recommendLoading">
      <el-card v-for="item in displayHouseList" :key="item.id" class="house-card" shadow="hover">
        <div class="cover-wrap">
          <el-image
            v-if="getFirstHouseImage(item)"
            :src="getFirstHouseImage(item)"
            fit="cover"
            class="cover-image"
            :preview-src-list="parseImageUrls(item.imageUrls)"
            preview-teleported
          />
          <div v-else class="cover-image placeholder">暂无图片</div>
        </div>

        <div class="house-card-top">
          <div>
            <div class="house-title">{{ item.title }}</div>
            <div class="house-location">{{ item.city }} · {{ item.area }} · {{ item.address }}</div>
          </div>
          <div class="house-price">¥ {{ item.rentPrice }}/月</div>
        </div>

        <div class="house-tags">
          <el-tag>{{ item.houseType || '户型待补充' }}</el-tag>
          <el-tag type="success">{{ item.square || '--' }}㎡</el-tag>
          <el-tag type="warning">{{ item.floor || '楼层待补充' }}</el-tag>
          <el-tag v-if="item.longitude && item.latitude" type="info">支持导航</el-tag>
          <el-tag v-if="item.recommendScore" type="danger">
            推荐分：{{ formatScore(item.recommendScore) }}
          </el-tag>
        </div>

        <div v-if="item.recommendReason" class="recommend-reason">
          推荐原因：{{ item.recommendReason }}
        </div>

        <div class="house-desc">{{ item.description || '暂无更多描述' }}</div>

        <div class="house-card-bottom">
          <span class="house-meta">发布时间：{{ item.createTime || '--' }}</span>
          <div class="action-group">
            <el-button @click="openMap(item)">导航</el-button>
            <el-button type="primary" @click="goDetail(item.id)">查看详情</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty
      v-if="!loading && !recommendLoading && displayHouseList.length === 0"
      :description="isRecommendMode ? '暂无推荐结果' : '没有符合条件的房源'"
    />

    <div class="table-pagination" v-if="!isRecommendMode && filteredHouseList.length > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[4, 8, 12]"
        background
        layout="total, sizes, prev, pager, next"
        :total="filteredHouseList.length"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getHouseListApi } from '../api/house'
import { recommendHouseApi } from '../api/recommend'
import { getFirstHouseImage, parseImageUrls } from '../utils/house'

const router = useRouter()
const tableData = ref([])
const loading = ref(false)

const keyword = ref('')
const cityFilter = ref('')
const areaFilter = ref('')
const houseTypeFilter = ref('')
const priceFilter = ref('')
const sortType = ref('latest')
const page = ref(1)
const pageSize = ref(8)

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

const isRecommendMode = computed(() => recommendList.value.length > 0)

const loadHouseList = async () => {
  loading.value = true
  try {
    const res = await getHouseListApi()
    tableData.value = res.data || []
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

    recommendList.value = (res.data || []).map((item) => ({
      ...item.house,
      recommendScore: item.score,
      recommendReason: item.reason,
      recommendModelType: item.modelType,
    }))

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
  recommendList.value = []
}

const cityOptions = computed(() => [...new Set(tableData.value.map((item) => item.city).filter(Boolean))])
const areaOptions = computed(() => [...new Set(tableData.value.map((item) => item.area).filter(Boolean))])
const houseTypeOptions = computed(() => [...new Set(tableData.value.map((item) => item.houseType).filter(Boolean))])

const filteredHouseList = computed(() => {
  const keywordValue = keyword.value.trim()
  const list = tableData.value.filter((item) => {
    const matchKeyword = !keywordValue
      || item.title?.includes(keywordValue)
      || item.address?.includes(keywordValue)
      || item.description?.includes(keywordValue)
    const matchCity = !cityFilter.value || item.city === cityFilter.value
    const matchArea = !areaFilter.value || item.area === areaFilter.value
    const matchHouseType = !houseTypeFilter.value || item.houseType === houseTypeFilter.value
    const price = Number(item.rentPrice || 0)
    const matchPrice = !priceFilter.value
      || (priceFilter.value === 'lt3000' && price < 3000)
      || (priceFilter.value === '3000-5000' && price >= 3000 && price <= 5000)
      || (priceFilter.value === '5000-8000' && price > 5000 && price <= 8000)
      || (priceFilter.value === 'gt8000' && price > 8000)

    return matchKeyword && matchCity && matchArea && matchHouseType && matchPrice
  })

  return list.sort((a, b) => {
    if (sortType.value === 'priceAsc') return Number(a.rentPrice || 0) - Number(b.rentPrice || 0)
    if (sortType.value === 'priceDesc') return Number(b.rentPrice || 0) - Number(a.rentPrice || 0)
    return Number(b.id || 0) - Number(a.id || 0)
  })
})

const pagedHouseList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredHouseList.value.slice(start, start + pageSize.value)
})

const displayHouseList = computed(() => {
  if (isRecommendMode.value) {
    return recommendList.value
  }
  return pagedHouseList.value
})

const resetFilter = () => {
  keyword.value = ''
  cityFilter.value = ''
  areaFilter.value = ''
  houseTypeFilter.value = ''
  priceFilter.value = ''
  sortType.value = 'latest'
  page.value = 1
}

const goDetail = (id) => {
  router.push(`/house/${id}`)
}

const openMap = (item) => {
  if (item.longitude && item.latitude) {
    const name = encodeURIComponent(item.title || '房屋位置')
    window.open(`https://uri.amap.com/navigation?to=${item.longitude},${item.latitude},${name}&mode=car&coordinate=gaode&src=house-rental-web&callnative=1`, '_blank')
    return
  }

  const address = encodeURIComponent([item.city, item.area, item.address].filter(Boolean).join(' '))
  window.open(`https://www.amap.com/search?query=${address}`, '_blank')
}

const formatScore = (score) => {
  return Number(score || 0).toFixed(1)
}

onMounted(() => {
  loadHouseList()
})
</script>

<style scoped>
.house-page {
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fa;
}

.banner {
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

.banner h2 {
  margin: 0 0 8px;
  font-size: 32px;
}

.banner p {
  margin: 0;
  color: #606266;
}

.banner-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-card {
  max-width: 1180px;
  margin: 0 auto 20px;
  border-radius: 18px;
}

.recommend-card {
  border: 1px solid #d9ecff;
}

.recommend-title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.recommend-title-row h3 {
  margin: 0 0 6px;
  font-size: 22px;
}

.recommend-title-row p {
  margin: 0;
  color: #606266;
  line-height: 1.7;
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

.model-select {
  width: 220px;
}

.topk-select {
  width: 140px;
}

.recommend-alert {
  margin-top: 14px;
}

.filter-card {
  max-width: 1180px;
  margin: 0 auto 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 150px;
}

.result-title {
  max-width: 1180px;
  margin: 0 auto 14px;
}

.result-title h3 {
  margin: 0 0 6px;
  font-size: 22px;
}

.result-title p {
  margin: 0;
  color: #606266;
}

.house-grid {
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.house-card {
  border-radius: 20px;
}

.cover-wrap {
  margin-bottom: 16px;
}

.cover-image {
  width: 100%;
  height: 220px;
  border-radius: 18px;
  overflow: hidden;
}

.cover-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f4f5;
  color: #909399;
}

.house-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.house-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.house-location {
  color: #606266;
}

.house-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
  white-space: nowrap;
}

.house-tags {
  margin: 18px 0 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.recommend-reason {
  margin: 8px 0 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f0f9eb;
  color: #529b2e;
  line-height: 1.6;
}

.house-desc {
  min-height: 44px;
  color: #606266;
  line-height: 1.7;
}

.house-card-bottom {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.house-meta {
  color: #909399;
  font-size: 14px;
}

.action-group {
  display: flex;
  gap: 10px;
}

.table-pagination {
  max-width: 1180px;
  margin: 20px auto 0;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .house-grid {
    grid-template-columns: 1fr;
  }

  .banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .recommend-title-row {
    flex-direction: column;
  }
}
</style>
