import { HealthContext } from './healthContext'
import { useEffect, useState } from 'react'
import { fetchHealth } from '../services/healthService'

export function HealthProvider({ children }) {
  const [status, setStatus] = useState('checking')

  useEffect(() => {
    const controller = new AbortController()

    fetchHealth(controller.signal)
      .then((health) => setStatus(health?.status === 'UP' ? 'online' : 'unavailable'))
      .catch(() => {
        if (!controller.signal.aborted) {
          setStatus('unavailable')
        }
      })

    return () => controller.abort()
  }, [])

  return <HealthContext.Provider value={{ status }}>{children}</HealthContext.Provider>
}
