import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
  }),

  actions: {
    setUserInfo(userInfo) {
      // 仅保存显示所需资料，任何后端兼容字段中的 token 都不能持久化。
      const profile = { ...userInfo }
      delete profile.token
      this.userInfo = profile
      localStorage.setItem('userInfo', JSON.stringify(profile))
    },

    clearUserInfo() {
      this.userInfo = null
      localStorage.removeItem('userInfo')
    },
  },
})
