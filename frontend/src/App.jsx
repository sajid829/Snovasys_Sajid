import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { HealthProvider } from './context/HealthProvider'
import AppLayout from './layouts/AppLayout'
import AccountPage from './pages/AccountPage'
import AdminUsersPage from './pages/AdminUsersPage'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import { ProtectedRoute, PublicOnlyRoute, RoleRoute } from './routes/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <HealthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
            </Route>
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="accounts/:accountId" element={<AccountPage />} />
                <Route element={<RoleRoute roles={['ADMIN']} />}>
                  <Route path="admin/users" element={<AdminUsersPage />} />
                </Route>
              </Route>
            </Route>
            <Route path="*" element={<LoginPage />} />
          </Routes>
        </BrowserRouter>
      </HealthProvider>
    </AuthProvider>
  )
}

export default App
