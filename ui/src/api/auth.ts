import { $get, $post } from './request'

export const MEMBER_ROLE = {
  OWNER: 1,
  MEMBER: 2,
} as const

export const MEMBER_STATUS = {
  DISABLED: 0,
  ENABLED: 1,
} as const

export interface LoginRequest {
  username: string
  password: string
}

export interface MemberInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  role?: number
  ownerId?: number | null
  token?: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  phone?: string
  shareCode: string
}

export async function login(body: LoginRequest) {
  return $post<MemberInfo>('/member/login', body)
}

export async function logout() {
  return $post('/member/logout')
}

export async function getMemberInfo() {
  return $get<MemberInfo>('/member/info')
}

export async function register(body: RegisterRequest) {
  return $post('/member/register', body)
}

export async function deleteMember(id: number) {
  return $post(`/member/delete/${id}`)
}
