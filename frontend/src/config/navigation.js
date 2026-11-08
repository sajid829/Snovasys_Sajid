const navigationItems = [
  { label: 'Overview', href: '/', state: 'available' },
  { label: 'Accounts', href: '/#accounts', state: 'available' },
  { label: 'User management', href: '/admin/users', roles: ['ADMIN'], state: 'available' },
]

export default navigationItems
