import { useCallback, useEffect, useState } from 'react'
import useAuth from '../hooks/useAuth'
import { fetchUsers, updateUser } from '../services/accountService'
import { getApiErrorMessage } from '../utils/apiError'

const roles = ['ACCOUNT_HOLDER', 'AUDITOR', 'ADMIN']

function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState(null)

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setUsers(await fetchUsers())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not load users.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchUsers({ signal: controller.signal })
      .then(setUsers)
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Could not load users.'))
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])

  async function saveAccess(userId, role, enabled) {
    setSavingId(userId)
    setError('')
    try {
      const updated = await updateUser(userId, { role, enabled })
      setUsers((current) => current.map((item) => item.id === userId ? updated : item))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not update this user.'))
    } finally {
      setSavingId(null)
    }
  }

  return (
    <section className="mx-auto max-w-6xl">
      <div className="mb-5">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-950">User management</h2>
        <p className="mt-1 text-sm text-slate-500">Role and account-status changes take effect immediately and revoke current tokens.</p>
      </div>
      {error && <p className="mb-4 rounded-xl bg-rose-50 p-4 text-sm text-rose-800" role="alert">{error}</p>}
      {loading && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500" role="status">Loading users…</p>}
      {!loading && !error && (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
          <table className="w-full min-w-[700px] text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr><th className="px-5 py-3">Email</th><th className="px-5 py-3">Role</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Actions</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users.map((item) => {
                const isCurrentAdmin = item.id === currentUser.id
                return (
                  <tr key={item.id}>
                    <td className="px-5 py-4 font-medium text-slate-800">{item.email}</td>
                    <td className="px-5 py-4">
                      <label className="sr-only" htmlFor={`role-${item.id}`}>Role for {item.email}</label>
                      <select
                        id={`role-${item.id}`}
                        value={item.role}
                        disabled={isCurrentAdmin || savingId === item.id}
                        onChange={(event) => saveAccess(item.id, event.target.value, item.enabled)}
                        className="rounded-lg border border-slate-300 bg-white px-2 py-1.5 disabled:bg-slate-100"
                      >
                        {roles.map((role) => <option key={role} value={role}>{role.replace('_', ' ')}</option>)}
                      </select>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{item.enabled ? 'Active' : 'Disabled'}</td>
                    <td className="px-5 py-4">
                      <button
                        type="button"
                        disabled={isCurrentAdmin || savingId === item.id}
                        onClick={() => saveAccess(item.id, item.role, !item.enabled)}
                        className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                      >
                        {savingId === item.id ? 'Saving…' : item.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {users.length === 0 && <p className="p-6 text-sm text-slate-500">No users found.</p>}
        </div>
      )}
      {!loading && error && <button type="button" onClick={loadUsers} className="mt-3 text-sm font-semibold text-emerald-800 underline">Try again</button>}
    </section>
  )
}

export default AdminUsersPage
