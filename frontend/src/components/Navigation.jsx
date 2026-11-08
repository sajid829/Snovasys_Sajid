import navigationItems from '../config/navigation'
import { Link } from 'react-router-dom'
import useAuth from '../hooks/useAuth'

function Navigation({ compact = false }) {
  const { user } = useAuth()
  const items = navigationItems.filter((item) => !item.roles || item.roles.includes(user?.role))

  return (
    <nav
      aria-label="Main navigation"
      className={compact ? 'flex gap-2 overflow-x-auto px-4 pb-3 lg:hidden' : 'space-y-1'}
    >
      {items.map(({ label, href, state }) => {
        const isCurrent = state === 'available'
        const baseClass = compact
          ? 'shrink-0 rounded-lg px-3 py-2 text-sm font-medium'
          : 'flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm font-medium'
        const stateClass = isCurrent
          ? 'bg-emerald-50 text-emerald-950'
          : 'text-slate-600 hover:bg-slate-50'

        return (
          <Link
            key={label}
            to={href}
            className={`${baseClass} ${stateClass}`}
            aria-current={isCurrent ? 'page' : undefined}
          >
            <span>{label}</span>
          </Link>
        )
      })}
    </nav>
  )
}

export default Navigation
