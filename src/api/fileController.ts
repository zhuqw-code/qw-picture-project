// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** uploadFile POST /api/file/upload */
export async function uploadFileUsingPost(
  body: {},
  multipartFile?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData()

  if (multipartFile) {
    formData.append('multipartFile', multipartFile)
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele]

    if (item !== undefined && item !== null) {
      if (typeof item === 'object' && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ''))
        } else {
          formData.append(ele, new Blob([JSON.stringify(item)], { type: 'application/json' }))
        }
      } else {
        formData.append(ele, item)
      }
    }
  })

  return request<API.BaseResultObject_>('/api/file/upload', {
    method: 'POST',
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}
