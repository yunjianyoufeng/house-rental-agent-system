import request from './request'

function getRealFile(file) {
  if (!file) {
    return null
  }

  // Element Plus el-upload 的 UploadFile 对象
  if (file.raw instanceof File) {
    return file.raw
  }

  // 原生 File 对象
  if (file instanceof File) {
    return file
  }

  return null
}

function buildUploadFormData(file, errorMessage) {
  const realFile = getRealFile(file)

  if (!realFile) {
    throw new Error(errorMessage || '请选择要上传的文件')
  }

  const formData = new FormData()
  formData.append('file', realFile)

  return formData
}

/**
 * 出租者上传房源图片
 */
export function uploadHouseImageApi(file) {
  return request({
    url: '/landlord/upload/image',
    method: 'post',
    data: buildUploadFormData(file, '请选择要上传的图片'),
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/**
 * 出租者上传合同附件
 */
export function uploadLandlordContractFileApi(file) {
  return request({
    url: '/landlord/upload/contract',
    method: 'post',
    data: buildUploadFormData(file, '请选择要上传的文件'),
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/**
 * 管理员上传合同附件
 */
export function uploadAdminContractFileApi(file) {
  return request({
    url: '/admin/upload/contract',
    method: 'post',
    data: buildUploadFormData(file, '请选择要上传的文件'),
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
