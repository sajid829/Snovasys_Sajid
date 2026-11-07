import useHealth from '../hooks/useHealth'

const labels = {
  checking: 'Checking API',
  online: 'API available',
  unavailable: 'API unavailable',
}

function HealthIndicator() {
  const { status } = useHealth()
  const isOnline = status === 'online'
  const isChecking = status === 'checking'

  return (
    <div
      className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600"
      role="status"
      aria-live="polite"
    >
      <span
        className={`size-2 rounded-full ${
          isOnline ? 'bg-emerald-500' : isChecking ? 'animate-pulse bg-amber-400' : 'bg-rose-500'
        }`}
        aria-hidden="true"
      />
      {labels[status]}
    </div>
  )
}

export default HealthIndicator
