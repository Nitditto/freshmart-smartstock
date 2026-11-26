import { BarChart3, ClipboardList, LayoutDashboard, LogOut, Menu, PackageMinus, Package, Sparkles } from 'lucide-react'
import { useState, type ComponentType } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import type { Role } from '../api/types'
import { useAuth } from '../shared/auth/AuthContext'
import { cn } from '../shared/lib/cn'
import { Badge } from '../shared/ui/Badge'
import { Button } from '../shared/ui/Button'

interface NavItem {
  to: string
  label: string
  icon: ComponentType<{ className?: string }>
  roles?: Role[]
  end?: boolean
}

const NAV: NavItem[] = [
  { to: '/', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/products', label: 'Sản phẩm', icon: Package },
  { to: '/receipts', label: 'Phiếu nhập', icon: ClipboardList, end: true },
  { to: '/receipts/parse', label: 'Nhập từ hóa đơn (AI)', icon: Sparkles },
  { to: '/issues', label: 'Phiếu xuất', icon: PackageMinus },
  { to: '/reports', label: 'Báo cáo tồn', icon: BarChart3, roles: ['ADMIN'] },
]

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  if (!user) return null
  const items = NAV.filter((item) => !item.roles || item.roles.includes(user.role))

  const onLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen md:flex">
      <aside
        className={cn(
          'w-full shrink-0 bg-primary text-white md:sticky md:top-0 md:block md:h-screen md:w-60',
          menuOpen ? 'block' : 'hidden',
        )}
      >
        <div className="px-5 py-4 text-lg font-bold tracking-tight">SmartStock</div>
        <nav aria-label="Menu chính" className="flex flex-col gap-1 px-3 pb-4">
          {items.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors duration-150',
                  isActive ? 'bg-white/15 font-semibold' : 'text-slate-200 hover:bg-white/10',
                )
              }
            >
              <Icon className="size-4" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-3 border-b border-border bg-card px-4 py-2">
          <button
            type="button"
            aria-label="Mở menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
            className="rounded-md p-2 text-foreground hover:bg-muted md:hidden"
          >
            <Menu className="size-5" aria-hidden="true" />
          </button>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">{user.fullName}</span>
            <Badge tone={user.role === 'ADMIN' ? 'info' : 'neutral'}>{user.role}</Badge>
            <Button variant="secondary" icon={<LogOut className="size-4" aria-hidden="true" />} onClick={onLogout}>
              Đăng xuất
            </Button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-5">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
