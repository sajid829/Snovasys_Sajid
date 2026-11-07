import useHealth from '../hooks/useHealth'

const summaryCards = [
  { label: 'Total balance', note: 'No account data available' },
  { label: 'Accounts', note: 'Account service not implemented' },
  { label: 'Transfers', note: 'Transfer service not implemented' },
]

function DashboardPage() {
  const { status } = useHealth()
  const apiMessage =
    status === 'online'
      ? 'The health endpoint responded. Financial services are not available yet.'
      : status === 'checking'
        ? 'Waiting for the backend health endpoint to respond.'
        : 'Start the backend to check connectivity. No financial data is being displayed.'

  return (
    <div className="mx-auto max-w-6xl">
      <section className="overflow-hidden rounded-3xl bg-emerald-950 px-6 py-8 text-white sm:px-9 sm:py-10">
        <div className="max-w-2xl">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-200">
            Money movement with a ledger you can prove
          </p>
          <h2 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">
            Clarity starts with a balanced ledger.
          </h2>
          <p className="mt-3 max-w-xl text-sm leading-6 text-emerald-100/80 sm:text-base">
            Your LedgerLine workspace is ready. Verified balances and activity will appear here once
            the account and transaction APIs are implemented.
          </p>
        </div>
        <div className="mt-8 inline-flex items-center gap-2 rounded-full border border-emerald-800 bg-emerald-900/70 px-3 py-2 text-xs font-medium text-emerald-100">
          <span className="size-2 rounded-full bg-emerald-300" aria-hidden="true" />
          Financial data is not connected
        </div>
      </section>

      <section aria-label="Account summary" className="mt-7 grid gap-4 sm:grid-cols-3">
        {summaryCards.map(({ label, note }) => (
          <article key={label} className="rounded-2xl border border-slate-200 bg-white p-5">
            <p className="text-sm font-medium text-slate-500">{label}</p>
            <p className="mt-4 text-2xl font-semibold tracking-tight text-slate-300" aria-label="Not available">
              —
            </p>
            <p className="mt-2 text-xs text-slate-500">{note}</p>
          </article>
        ))}
      </section>

      <section className="mt-7 grid gap-5 lg:grid-cols-[1.4fr_0.8fr]">
        <article className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-7">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-slate-900">Recent activity</h3>
              <p className="mt-1 text-sm text-slate-500">A traceable history will live here.</p>
            </div>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-medium text-slate-500">
              Not connected
            </span>
          </div>
          <div className="mt-7 rounded-xl border border-dashed border-slate-200 bg-slate-50 px-5 py-9 text-center">
            <p className="text-sm font-medium text-slate-700">No transaction data to display</p>
            <p className="mt-1 text-xs leading-5 text-slate-500">
              Activity will be loaded from the backend after ledger services are implemented.
            </p>
          </div>
        </article>

        <article className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-7">
          <h3 className="text-base font-semibold text-slate-900">System connection</h3>
          <p className="mt-1 text-sm text-slate-500">Connectivity is checked against the real API.</p>
          <div className="mt-5 rounded-xl bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Backend health</p>
            <p className="mt-2 text-sm leading-6 text-slate-700" role="status" aria-live="polite">
              {apiMessage}
            </p>
          </div>
          <p className="mt-4 text-xs leading-5 text-slate-500">
            This status confirms only the health endpoint, not database or financial transaction integrity.
          </p>
        </article>
      </section>
    </div>
  )
}

export default DashboardPage
