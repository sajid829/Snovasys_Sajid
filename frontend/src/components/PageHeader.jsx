import HealthIndicator from './HealthIndicator'
import useAuth from '../hooks/useAuth'
import { useNavigate, useLocation } from 'react-router-dom'

function PageHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const title = location.pathname.startsWith('/admin') ? 'User management' : location.pathname.startsWith('/accounts/') ? 'Account details' : 'Financial overview'

  async function handleSignOut() {
    try {
      await logout()
    } catch {
      navigate('/login', {
        replace: true,
        state: { logoutWarning: 'Signed out on this device, but server logout could not be confirmed. The token will expire automatically.' },
      })
    }
  }

  return (
    <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200/80 bg-white px-4 py-4 sm:px-8">
      <div>
        <p className="text-xs font-medium text-slate-500">Workspace / Overview</p>
        <h1 className="mt-1 text-lg font-semibold tracking-tight text-slate-950">{title}</h1>
      </div>
      <div className="flex items-center gap-3">
        <HealthIndicator />
        <span className="hidden text-sm text-slate-600 sm:inline">{user?.email}</span>
        <button
          type="button"
          onClick={handleSignOut}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          Sign out
        </button>
      </div>
    </header>
  )
}

export default PageHeader
