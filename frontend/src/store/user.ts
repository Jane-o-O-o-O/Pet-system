import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(r: string, id: string | number, name: string) {
    role.value = r
    userId.value = String(id)
    username.value = name
    localStorage.setItem('role', r)
    localStorage.setItem('userId', String(id))
    localStorage.setItem('username', name)
  }

  function logout() {
    token.value = ''
    role.value = ''
    userId.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
  }

  return { token, role, userId, username, setToken, setUserInfo, logout }
})
