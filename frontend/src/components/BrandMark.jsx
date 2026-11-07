function BrandMark() {
  return (
    <div className="flex items-center gap-3" aria-label="LedgerLine">
      <span className="flex size-10 items-center justify-center rounded-xl bg-emerald-950 text-emerald-50">
        <svg viewBox="0 0 24 24" fill="none" className="size-6" aria-hidden="true">
          <path d="M7 3v18h14" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
          <path d="M12 15h9" stroke="#9ED7B8" strokeLinecap="round" strokeWidth="2.5" />
        </svg>
      </span>
      <span className="text-lg font-semibold tracking-tight text-slate-950">
        Ledger<span className="text-emerald-800">Line</span>
      </span>
    </div>
  )
}

export default BrandMark
