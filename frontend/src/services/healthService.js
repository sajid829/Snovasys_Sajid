import apiClient from './apiClient'

export async function fetchHealth(signal) {
  const response = await apiClient.get('/health', { signal })
  return response.data
}
