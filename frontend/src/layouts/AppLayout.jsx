import BrandMark from '../components/BrandMark'
import Navigation from '../components/Navigation'
import PageHeader from '../components/PageHeader'
import { Outlet } from 'react-router-dom'

function AppLayout() {
  return (
    <div className="min-h-screen bg-[#f7f8fa] text-slate-950">
      <div className="mx-auto flex min-h-screen max-w-[1600px]">
        <aside className="hidden w-64 shrink-0 flex-col border-r border-slate-200 bg-white px-5 py-6 lg:flex">
          <BrandMark />
          <p className="mb-3 mt-10 px-3 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
            Workspace
          </p>
          <Navigation />
          <div className="mt-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-semibold text-slate-800">A clear view of every entry.</p>
            <p className="mt-1 text-xs leading-5 text-slate-500">
              Account data will appear here when the secure ledger APIs are ready.
            </p>
          </div>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <div className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-4 lg:hidden">
            <BrandMark />
            <span className="text-xs font-medium text-slate-500">Day 1 foundation</span>
          </div>
          <Navigation compact />
          <PageHeader />
          <main className="w-full flex-1 px-4 py-7 sm:px-8 sm:py-9">
            <Outlet />
          </main>
          <footer className="border-t border-slate-200 px-4 py-5 text-center text-xs text-slate-500 sm:px-8">
            LedgerLine · Financial activity will be shown only when confirmed by the backend.
          </footer>
        </div>
      </div>
    </div>
  )
}

export default AppLayout
