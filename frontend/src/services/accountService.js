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

export async function fetchAccountTransactions(accountId, options = {}) {
  const response = await apiClient.get(
    `/accounts/${encodeURIComponent(accountId)}/transactions`,
    options,
  )
  return response.data
}

export async function submitDeposit(accountId, amount, idempotencyKey) {
  const numericAmount = serializeAmount(amount)
  const response = await apiClient.post(
    `/accounts/${encodeURIComponent(accountId)}/deposits`,
    `{"amount":${numericAmount}}`,
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
    },
  )
  return response.data
}

export async function submitWithdrawal(accountId, amount, idempotencyKey) {
  const numericAmount = serializeAmount(amount)
  const response = await apiClient.post(
    `/accounts/${encodeURIComponent(accountId)}/withdrawals`,
    `{"amount":${numericAmount}}`,
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
    },
  )
  return response.data
}

export async function submitTransfer(sourceAccountId, destinationAccountId, amount, idempotencyKey) {
  const numericAmount = serializeAmount(amount)
  const response = await apiClient.post(
    '/transfers',
    `{"sourceAccountId":${JSON.stringify(sourceAccountId)},`
      + `"destinationAccountId":${JSON.stringify(destinationAccountId)},`
      + `"amount":${numericAmount}}`,
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
    },
  )
  return response.data
}

function serializeAmount(amount) {
  if (!/^(?:0|[1-9]\d{0,16})(?:\.\d{1,2})?$/.test(amount) || !/[1-9]/.test(amount)) {
    throw new Error('Enter a positive amount with up to 17 whole-number digits and 2 decimals.')
  }
  return amount
}

export async function fetchUsers(options = {}) {
  const response = await apiClient.get('/admin/users', options)
  return response.data
}

export async function updateUser(userId, access) {
  const response = await apiClient.patch(`/admin/users/${encodeURIComponent(userId)}`, access)
  return response.data
}
