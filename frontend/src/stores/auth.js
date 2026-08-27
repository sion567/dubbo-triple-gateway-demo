import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roles: (s) => s.user?.roles || [],
    isAdmin() {
      return this.roles.includes('ROLE_ADMIN')
    },
  },
  actions: {
    login(payload) {
      this.token = payload.token
      this.user = { username: payload.username, roles: payload.roles, perms: payload.perms }
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
