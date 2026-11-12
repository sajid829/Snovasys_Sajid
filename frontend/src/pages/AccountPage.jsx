import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import {
  fetchAccount,
  fetchAccountTransactions,
  fetchAccounts,
  submitDeposit,
  submitTransfer,
  submitWithdrawal,
} from '../services/accountService'
import { getApiErrorMessage } from '../utils/apiError'
import { formatMoney } from '../utils/formatMoney'

function AccountPage() {
  const { accountId } = useParams()
  const { user } = useAuth()
  const [result, setResult] = useState(null)
  const [action, setAction] = useState('DEPOSIT')
  const [destinationAccountId, setDestinationAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [notice, setNotice] = useState('')
  const [refreshError, setRefreshError] = useState('')
  const pendingRequest = useRef(null)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      fetchAccount(accountId, { signal: controller.signal }),
      fetchAccountTransactions(accountId, { signal: controller.signal }),
      user.role === 'ACCOUNT_HOLDER' ? fetchAccounts({ signal: controller.signal }) : Promise.resolve([]),
    ])
      .then(([account, transactions, accounts]) => {
        setResult({ accountId, account, transactions, accounts, error: '' })
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setResult({
            accountId,
            account: null,
            transactions: [],
            accounts: [],
            error: getApiErrorMessage(requestError, 'Could not load this account.'),
          })
        }
      })
    return () => controller.abort()
  }, [accountId, user.role])

  const currentResult = result?.accountId === accountId ? result : null
  const loading = currentResult === null
  const account = currentResult?.account
  const transactions = currentResult?.transactions ?? []
  const otherAccounts = (currentResult?.accounts ?? []).filter((item) => item.id !== accountId)
  const error = currentResult?.error
  const canTransact = user.role === 'ACCOUNT_HOLDER' && account?.status === 'ACTIVE'

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setSubmitError('')
    setNotice('')
    setRefreshError('')

    try {
      const requestFingerprint = JSON.stringify([action, accountId, destinationAccountId, amount])
      if (pendingRequest.current?.fingerprint !== requestFingerprint) {
        pendingRequest.current = { fingerprint: requestFingerprint, key: crypto.randomUUID() }
      }
      const idempotencyKey = pendingRequest.current.key
      let transaction
      if (action === 'DEPOSIT') {
        transaction = await submitDeposit(accountId, amount, idempotencyKey)
      } else if (action === 'WITHDRAWAL') {
        transaction = await submitWithdrawal(accountId, amount, idempotencyKey)
      } else {
        if (!destinationAccountId || destinationAccountId === accountId) {
          setSubmitError('Choose a different destination account.')
          return
        }
        transaction = await submitTransfer(accountId, destinationAccountId, amount, idempotencyKey)
      }

      pendingRequest.current = null
      setAmount('')
      setNotice(`${actionLabel(action)} completed. Reference: ${transaction.id}`)
      try {
        const [updatedAccount, updatedTransactions] = await Promise.all([
          fetchAccount(accountId),
          fetchAccountTransactions(accountId),
        ])
        setResult((current) => current?.accountId === accountId
          ? { ...current, account: updatedAccount, transactions: updatedTransactions }
          : current)
      } catch (requestError) {
        setRefreshError(getApiErrorMessage(requestError, 'The transaction completed, but account data could not refresh.'))
      }
    } catch (requestError) {
      setSubmitError(getApiErrorMessage(requestError, 'The transaction could not be completed.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl">
      <Link to="/" className="text-sm font-semibold text-emerald-800 hover:underline">← Back to accounts</Link>
      <h2 className="mt-5 text-2xl font-semibold tracking-tight text-slate-950">Account details</h2>
      {loading && <p className="mt-5 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500" role="status">Loading account…</p>}
      {error && <p className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-800" role="alert">{error}</p>}
      {account && !loading && (
        <section className="mt-5 rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-sm text-slate-500">{account.accountType.toLowerCase()} account</p>
              <p className="mt-1 font-mono text-lg text-slate-800">{account.accountNumber}</p>
            </div>
            <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-medium text-emerald-800">
              {account.status.toLowerCase()}
            </span>
          </div>
          <p className="mt-8 text-sm text-slate-500">Current balance</p>
          <p className="mt-2 text-4xl font-semibold tracking-tight text-slate-950">
            {formatMoney(account.currency, account.balance)}
          </p>
          <dl className="mt-8 grid gap-4 border-t border-slate-100 pt-5 sm:grid-cols-2">
            <div>
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Currency</dt>
              <dd className="mt-1 text-sm text-slate-800">{account.currency}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Opened</dt>
              <dd className="mt-1 text-sm text-slate-800">{new Date(account.createdAt).toLocaleString()}</dd>
            </div>
          </dl>

          {canTransact && (
            <form onSubmit={handleSubmit} className="mt-8 border-t border-slate-100 pt-6">
              <h3 className="text-lg font-semibold text-slate-950">Move money</h3>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <label className="text-sm font-medium text-slate-700">
                  Transaction
                  <select
                    value={action}
                    onChange={(event) => setAction(event.target.value)}
                    className="mt-1.5 block w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"
                  >
                    <option value="DEPOSIT">Deposit</option>
                    <option value="WITHDRAWAL">Withdraw</option>
                    <option value="TRANSFER">Transfer</option>
                  </select>
                </label>
                <label className="text-sm font-medium text-slate-700">
                  Amount ({account.currency})
                  <input
                    required
                    inputMode="decimal"
                    autoComplete="off"
                    value={amount}
                    onChange={(event) => setAmount(event.target.value)}
                    pattern="(?:0|[1-9][0-9]{0,16})(?:\.[0-9]{1,2})?"
                    title="Enter a positive amount with up to 2 decimal places."
                    className="mt-1.5 block w-full rounded-xl border border-slate-300 px-3 py-2.5"
                  />
                </label>
                {action === 'TRANSFER' && (
                  <label className="text-sm font-medium text-slate-700 sm:col-span-2">
                    Destination account
                    <select
                      required
                      value={destinationAccountId}
                      onChange={(event) => setDestinationAccountId(event.target.value)}
                      className="mt-1.5 block w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"
                    >
                      <option value="">Select an account</option>
                      {otherAccounts.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.accountType.toLowerCase()} •••• {item.accountNumber.slice(-4)}
                        </option>
                      ))}
                    </select>
                    {otherAccounts.length === 0 && (
                      <span className="mt-1 block text-xs text-slate-500">Open another account to make a transfer.</span>
                    )}
                  </label>
                )}
              </div>
              {submitError && <p className="mt-3 text-sm text-rose-800" role="alert">{submitError}</p>}
              {notice && <p className="mt-3 break-all text-sm text-emerald-800" role="status">{notice}</p>}
              {refreshError && <p className="mt-2 text-sm text-amber-800" role="status">{refreshError}</p>}
              <button
                type="submit"
                disabled={submitting || (action === 'TRANSFER' && otherAccounts.length === 0)}
                className="mt-4 rounded-xl bg-emerald-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-900 disabled:opacity-60"
              >
                {submitting ? 'Processing…' : `${actionLabel(action)}`}
              </button>
            </form>
          )}

          <div className="mt-8 border-t border-slate-100 pt-6">
            <h3 className="text-lg font-semibold text-slate-950">Transaction history</h3>
            {transactions.length === 0 && (
              <p className="mt-3 text-sm text-slate-500">No transactions recorded for this account.</p>
            )}
            {transactions.length > 0 && (
              <ul className="mt-3 divide-y divide-slate-100">
                {transactions.map((transaction) => (
                  <li key={transaction.id} className="flex flex-wrap items-center justify-between gap-3 py-4">
                    <div>
                      <p className="font-medium text-slate-800">
                        {transaction.type.toLowerCase()} · {transaction.direction.toLowerCase()}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        {transaction.createdAt ? new Date(transaction.createdAt).toLocaleString() : 'Date unavailable'}
                        {' · '}Ref {transaction.id.slice(0, 8)}
                        {transaction.type === 'TRANSFER' && ` · ${transaction.counterpartyAccount}`}
                      </p>
                    </div>
                    <p className={`font-semibold ${transaction.direction === 'CREDIT' ? 'text-emerald-800' : 'text-slate-800'}`}>
                      {transaction.direction === 'CREDIT' ? '+' : '−'}
                      {formatMoney(transaction.currency, transaction.amount)}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      )}
    </div>
  )
}

function actionLabel(action) {
  if (action === 'WITHDRAWAL') return 'Withdraw'
  if (action === 'TRANSFER') return 'Transfer'
  return 'Deposit'
}

export default AccountPage
