import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { createAccount, fetchAccounts } from '../services/accountService'
import { getApiErrorMessage } from '../utils/apiError'
import { formatMoney } from '../utils/formatMoney'

function DashboardPage() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [accountType, setAccountType] = useState('CHECKING')
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')

  const loadAccounts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setAccounts(await fetchAccounts())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not load accounts. Please try again.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchAccounts({ signal: controller.signal })
      .then(setAccounts)
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Could not load accounts. Please try again.'))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])

  async function handleCreateAccount(event) {
    event.preventDefault()
    setCreating(true)
    setFormError('')
    try {
      const account = await createAccount(accountType)
      setAccounts((current) => [account, ...current])
    } catch (requestError) {
      setFormError(getApiErrorMessage(requestError, 'Could not create the account. Please try again.'))
    } finally {
      setCreating(false)
    }
  }

  const canCreateAccount = user.role === 'ACCOUNT_HOLDER'
  const roleLabel = user.role === 'ACCOUNT_HOLDER' ? 'Account holder' : user.role.toLowerCase()

  return (
    <div className="mx-auto max-w-6xl">
      <section className="overflow-hidden rounded-3xl bg-emerald-950 px-6 py-8 text-white sm:px-9 sm:py-10">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-200">Secure financial workspace</p>
        <h2 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">Welcome, {user.email}</h2>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-emerald-100/80 sm:text-base">
          Signed in as {roleLabel}. Account information below is loaded from the LedgerLine API.
        </p>
      </section>

      <section id="accounts" className="mt-7 scroll-mt-6">
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-slate-950">Accounts</h2>
            <p className="mt-1 text-sm text-slate-500">
              {user.role === 'ACCOUNT_HOLDER' ? 'Only your accounts are shown.' : 'Accounts visible to your read permissions.'}
            </p>
          </div>
          {canCreateAccount && (
            <form onSubmit={handleCreateAccount} className="flex flex-wrap items-center gap-2">
              <label className="sr-only" htmlFor="account-type">Account type</label>
              <select
                id="account-type"
                value={accountType}
                onChange={(event) => setAccountType(event.target.value)}
                className="rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm"
              >
                <option value="CHECKING">Checking</option>
                <option value="SAVINGS">Savings</option>
              </select>
              <button
                type="submit"
                disabled={creating}
                className="rounded-xl bg-emerald-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-900 disabled:opacity-60"
              >
                {creating ? 'Opening…' : 'Open account'}
              </button>
            </form>
          )}
        </div>

        {formError && <p className="mb-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert">{formError}</p>}
        {error && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-800" role="alert">
            <p>{error}</p>
            <button type="button" onClick={loadAccounts} className="mt-2 font-semibold underline">Try again</button>
          </div>
        )}
        {loading && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500" role="status">Loading accounts…</p>}
        {!loading && !error && accounts.length === 0 && (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-10 text-center">
            <p className="font-medium text-slate-800">No accounts found</p>
            <p className="mt-1 text-sm text-slate-500">
              {canCreateAccount ? 'Open an account to see its verified balance here.' : 'No account records are available to your role.'}
            </p>
          </div>
        )}
        {!loading && !error && accounts.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2">
            {accounts.map((account) => (
              <article key={account.id} className="rounded-2xl border border-slate-200 bg-white p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium text-slate-500">{account.accountType.toLowerCase()} account</p>
                    <p className="mt-1 font-mono text-sm text-slate-700">•••• {account.accountNumber.slice(-4)}</p>
                  </div>
                  <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-800">
                    {account.status.toLowerCase()}
                  </span>
                </div>
                <p className="mt-5 text-2xl font-semibold tracking-tight text-slate-950">
                  {formatMoney(account.currency, account.balance)}
                </p>
                <Link
                  to={`/accounts/${encodeURIComponent(account.id)}`}
                  className="mt-4 inline-flex text-sm font-semibold text-emerald-800 hover:underline"
                >
                  View account details
                </Link>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default DashboardPage
