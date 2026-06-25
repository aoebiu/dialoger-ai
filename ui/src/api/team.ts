import { $get, $post, $put } from './request'
import { MEMBER_ROLE, MEMBER_STATUS } from './auth'

export interface TeamMember {
  id: number
  username: string
  nickname?: string
  phone?: string
  status?: number
  role?: number
  createdAt?: string
}

export interface TeamOverview {
  owner: TeamMember
  members: TeamMember[]
  currentUserId: number
}

export interface CreateTeamMemberRequest {
  username: string
  password: string
  nickname?: string
  phone?: string
}

export interface UpdateTeamMemberRequest {
  nickname?: string
  phone?: string
  status?: number
  password?: string
}

export function statusLabel(status?: number): string {
  if (status === MEMBER_STATUS.ENABLED) return '正常'
  if (status === MEMBER_STATUS.DISABLED) return '禁用'
  return status?.toString() ?? '未知'
}

export function statusBadgeClass(status?: number): string {
  if (status === MEMBER_STATUS.ENABLED) return 'status-normal'
  if (status === MEMBER_STATUS.DISABLED) return 'status-disabled'
  return ''
}

export function roleLabel(role?: number): string {
  if (role === MEMBER_ROLE.OWNER) return 'Owner'
  if (role === MEMBER_ROLE.MEMBER) return 'Member'
  return '未知'
}

export function roleBadgeClass(role?: number): string {
  if (role === MEMBER_ROLE.OWNER) return 'role-owner'
  if (role === MEMBER_ROLE.MEMBER) return 'role-member'
  return 'role-unknown'
}

export async function getTeamOverview() {
  return $get<TeamOverview>('/member/team/overview')
}

export async function createTeamMember(body: CreateTeamMemberRequest) {
  return $post<TeamMember>('/member/team/member', body)
}

export async function updateTeamMember(id: number, body: UpdateTeamMemberRequest) {
  return $put(`/member/team/member/${id}`, body)
}

export async function disableTeamMember(id: number) {
  return $post(`/member/team/member/${id}/disable`)
}
