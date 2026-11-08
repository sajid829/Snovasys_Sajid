import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchAccount } from '../services/accountService'
import { getApiErrorMessage } from '../utils/apiError'
import { formatMoney } from '../utils/formatMoney'

function AccountPage() {
  const { accountId } = useParams()
  const [result, setResult] = useState(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchAccount(accountId, { signal: controller.signal })
      .then((account) => setResult({ accountId, account, error: '' }))
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setResult({
            accountId,
            account: null,
            error: getApiErrorMessage(requestError, 'Could not load this account.'),
          })
        }
      })
    return () => controller.abort()
  }, [accountId])
  const currentResult = result?.accountId === accountId ? result : null
  const loading = currentResult === null
  const account = currentResult?.account
  const error = currentResult?.error

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
          <p className="mt-6 text-xs leading-5 text-slate-500">
            No deposits, withdrawals, or transfers are available yet. The displayed balance is read from the backend.
          </p>
        </section>
      )}
    </div>
  )
}

export default AccountPage
