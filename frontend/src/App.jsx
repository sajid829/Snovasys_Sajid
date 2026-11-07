import { HealthProvider } from './context/HealthProvider'
import AppLayout from './layouts/AppLayout'
import DashboardPage from './pages/DashboardPage'

function App() {
  return (
    <HealthProvider>
      <AppLayout>
        <DashboardPage />
      </AppLayout>
    </HealthProvider>
  )
}

export default App
