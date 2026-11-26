import { CheckCircle2, Info, XCircle } from 'lucide-react'
import { useSyncExternalStore } from 'react'
import { cn } from '../lib/cn'
import { dismiss, getToasts, subscribeToasts, type ToastKind } from './toast'

const STYLES: Record<ToastKind, string> = {
  success: 'border-accent bg-success-soft text-accent',
  error: 'border-danger bg-danger-soft text-danger',
  info: 'border-border bg-card text-foreground',
}

const ICONS = { success: CheckCircle2, error: XCircle, info: Info }

export function ToastViewport() {
  const toasts = useSyncExternalStore(subscribeToasts, getToasts)
  return (
    <div aria-live="polite" className="fixed right-4 bottom-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2">
      {toasts.map((t) => {
        const Icon = ICONS[t.kind]
        return (
          <div
            key={t.id}
            role={t.kind === 'error' ? 'alert' : 'status'}
            className={cn('flex items-start gap-2 rounded-md border px-3 py-2 text-sm', STYLES[t.kind])}
          >
            <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span className="flex-1">{t.text}</span>
            <button type="button" onClick={() => dismiss(t.id)} aria-label="Đóng thông báo" className="text-xs underline">
              Đóng
            </button>
          </div>
        )
      })}
    </div>
  )
}
