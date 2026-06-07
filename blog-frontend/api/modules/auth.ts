import { api } from '~/api/index'
import type { LoginRequest, LoginResponse } from '~/types'

export function register(data: LoginRequest) {
  return api.post<LoginResponse>('/api/auth/register', data)
}

export function login(data: LoginRequest) {
  return api.post<LoginResponse>('/api/auth/login', data)
}

export function logout() {
  return api.post<void>('/api/auth/logout')
}
