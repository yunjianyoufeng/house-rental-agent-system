export function parseImageUrls(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)

  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) return []

    try {
      const parsed = JSON.parse(trimmed)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch {
      // ignore and fallback to comma/newline split
    }

    return trimmed
      .split(/[\n,]/)
      .map((item) => item.trim())
      .filter(Boolean)
  }

  return []
}

export function stringifyImageUrls(list) {
  return JSON.stringify((list || []).filter(Boolean))
}

export function getFirstHouseImage(house) {
  return parseImageUrls(house?.imageUrls)[0] || ''
}

export function formatHouseAuditStatus(auditStatus) {
  if (auditStatus === 0) return '待审核'
  if (auditStatus === 1) return '已通过'
  if (auditStatus === 2) return '已拒绝'
  return '--'
}

export function formatHouseStatus(status) {
  if (status === 0) return '已下架'
  if (status === 1) return '已上架'
  if (status === 2) return '待支付'
  if (status === 3) return '已出租'
  return '--'
}
