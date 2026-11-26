import { zodResolver } from '@hookform/resolvers/zod'
import { LogIn } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ApiError } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { Button } from '../../shared/ui/Button'
import { Card } from '../../shared/ui/Card'
import { TextField } from '../../shared/ui/Field'

const schema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
})
type LoginForm = z.infer<typeof schema>

/** Seeded accounts of the `dev` profile (README). The password is never hard-coded: it comes from VITE_DEMO_PASSWORD. */
const DEMO_ACCOUNTS = [
  { label: 'ADMIN', email: 'admin@freshmart.local' },
  { label: 'STAFF 1', email: 'staff1@freshmart.local' },
  { label: 'STAFF 2', email: 'staff2@freshmart.local' },
]
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD ?? ''

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } })

  if (user) {
    return <Navigate to="/" replace />
  }

  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/'

  const onSubmit = async (values: LoginForm) => {
    setError(null)
    try {
      await login(values.email, values.password)
      navigate(redirectTo, { replace: true })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Không đăng nhập được. Vui lòng thử lại.')
    }
  }

  const fillDemo = (email: string) => {
    setValue('email', email, { shouldValidate: true })
    if (DEMO_PASSWORD) setValue('password', DEMO_PASSWORD, { shouldValidate: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <Card className="w-full max-w-sm p-6">
        <h1 className="text-xl font-bold">SmartStock</h1>
        <p className="mt-1 mb-5 text-sm text-muted-foreground">Đăng nhập để quản lý kho FreshMart</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <TextField label="Email" type="email" autoComplete="username" error={errors.email?.message} {...register('email')} />
          <TextField
            label="Mật khẩu"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          {error && (
            <p role="alert" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
              {error}
            </p>
          )}
          <Button type="submit" loading={isSubmitting} icon={<LogIn className="size-4" aria-hidden="true" />}>
            Đăng nhập
          </Button>
        </form>

        <div className="mt-5 border-t border-border pt-4">
          <p className="mb-2 text-xs text-muted-foreground">Tài khoản mẫu (môi trường demo)</p>
          <div className="flex flex-wrap gap-2">
            {DEMO_ACCOUNTS.map((account) => (
              <Button key={account.email} variant="secondary" onClick={() => fillDemo(account.email)}>
                {account.label}
              </Button>
            ))}
          </div>
          {!DEMO_PASSWORD && (
            <p className="mt-2 text-xs text-muted-foreground">Mật khẩu mẫu xem trong README của dự án.</p>
          )}
        </div>
      </Card>
    </div>
  )
}
