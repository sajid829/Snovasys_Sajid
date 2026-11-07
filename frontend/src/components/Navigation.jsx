import navigationItems from '../config/navigation'

function Navigation({ compact = false }) {
  return (
    <nav
      aria-label="Main navigation"
      className={compact ? 'flex gap-2 overflow-x-auto px-4 pb-3 lg:hidden' : 'space-y-1'}
    >
      {navigationItems.map(({ label, state }) => {
        const isCurrent = state === 'available'
        const baseClass = compact
          ? 'shrink-0 rounded-lg px-3 py-2 text-sm font-medium'
          : 'flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm font-medium'
        const stateClass = isCurrent
          ? 'bg-emerald-50 text-emerald-950'
          : 'text-slate-400'

        return (
          <button
            key={label}
            type="button"
            className={`${baseClass} ${stateClass}`}
            aria-current={isCurrent ? 'page' : undefined}
            aria-disabled={!isCurrent}
            disabled={!isCurrent}
          >
            <span>{label}</span>
            {!isCurrent && !compact && (
              <span className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                Soon
              </span>
            )}
          </button>
        )
      })}
    </nav>
  )
}

export default Navigation
