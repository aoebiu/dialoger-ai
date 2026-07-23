import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { MemberInfo } from '@/api/auth'
import { MEMBER_ROLE } from '@/api/auth'
import * as authApi from '@/api/auth'
import { useConversationStore } from '@/stores/conversation'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  let initialUser: MemberInfo | null = null
  try {
    const raw = localStorage.getItem(USER_KEY)
    initialUser = raw ? (JSON.parse(raw) as MemberInfo) : null
  } catch {
    /* ignore */
  }
  const user = ref<MemberInfo | null>(initialUser)

  const isLoggedIn = computed(() => !!token.value)

  const isOwner = computed(() => {
    const role = user.value?.role
    return role === undefined || role === MEMBER_ROLE.OWNER
  })

  const isMember = computed(() => user.value?.role === MEMBER_ROLE.MEMBER)

  function setAuth(t: string, u: MemberInfo) {
    token.value = t
    user.value = u
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    useConversationStore().reset()
  }

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    if (!res.success || !res.data) {
      throw new Error(res.message || '登录失败')
    }
    const data = res.data
    setAuth(data.token || token.value || '', data)
    return data
  }

  async function refreshUserInfo() {
    if (!token.value) return null
    const res = await authApi.getMemberInfo()
    if (!res.success || !res.data) {
      throw new Error(res.message || '获取用户信息失败')
    }
    setAuth(token.value, { ...res.data, token: token.value })
    return res.data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }

  return { token, user, isLoggedIn, isOwner, isMember, setAuth, clearAuth, login, logout, refreshUserInfo }
})
