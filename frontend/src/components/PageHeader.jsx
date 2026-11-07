import HealthIndicator from './HealthIndicator'

function PageHeader() {
  return (
    <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200/80 bg-white px-4 py-4 sm:px-8">
      <div>
        <p className="text-xs font-medium text-slate-500">Workspace / Overview</p>
        <h1 className="mt-1 text-lg font-semibold tracking-tight text-slate-950">Financial overview</h1>
      </div>
      <HealthIndicator />
    </header>
  )
}

export default PageHeader
