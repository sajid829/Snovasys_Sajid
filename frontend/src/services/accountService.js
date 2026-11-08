import apiClient from './apiClient'

export async function fetchAccounts(options = {}) {
  const response = await apiClient.get('/accounts', options)
  return response.data
}

export async function fetchAccount(accountId, options = {}) {
  const response = await apiClient.get(`/accounts/${encodeURIComponent(accountId)}`, options)
  return response.data
}

export async function createAccount(accountType) {
  const response = await apiClient.post('/accounts', { accountType })
  return response.data
}

export async function fetchUsers(options = {}) {
  const response = await apiClient.get('/admin/users', options)
  return response.data
}

export async function updateUser(userId, access) {
  const response = await apiClient.patch(`/admin/users/${encodeURIComponent(userId)}`, access)
  return response.data
}
