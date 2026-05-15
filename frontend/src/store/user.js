import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  async function login(username, password) {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // ignore
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function getUserInfo() {
    const res = await getUserInfoApi()
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(res.data))
  }

  return { token, userInfo, login, logout, getUserInfo }
})
