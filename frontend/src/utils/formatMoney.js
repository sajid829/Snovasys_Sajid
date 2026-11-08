function groupIndianDigits(digits) {
  if (digits.length <= 3) return digits
  const lastThree = digits.slice(-3)
  const remaining = digits.slice(0, -3).replace(/\B(?=(\d{2})+(?!\d))/g, ',')
  return `${remaining},${lastThree}`
}

export function formatMoney(currency, amount) {
  const [whole = '0', fraction = '00'] = String(amount).split('.')
  const negative = whole.startsWith('-')
  const digits = negative ? whole.slice(1) : whole
  return `${currency} ${negative ? '-' : ''}${groupIndianDigits(digits)}.${fraction.padEnd(2, '0')}`
}
