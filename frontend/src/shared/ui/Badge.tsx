import type { ReactNode } from 'react'
import type { DocumentStatus } from '../../api/types'
import { cn } from '../lib/cn'
import { STATUS_LABEL } from '../lib/format'

export type Tone = 'neutral' | 'success' | 'danger' | 'warning' | 'info'

const TONES: Record<Tone, string> = {
  neutral: 'bg-muted text-muted-foreground',
  success: 'bg-success-soft text-accent',
  danger: 'bg-danger-soft text-danger',
  warning: 'bg-warning-soft text-warning',
  info: 'bg-sky-50 text-sky-800',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap',
        TONES[tone],
      )}
    >
      {children}
    </span>
  )
}

const STATUS_TONE: Record<DocumentStatus, Tone> = { DRAFT: 'neutral', CONFIRMED: 'success', CANCELLED: 'danger' }

/** Status is always colour plus text, never colour alone. */
export function StatusBadge({ status }: { status: DocumentStatus }) {
  return <Badge tone={STATUS_TONE[status]}>{STATUS_LABEL[status]}</Badge>
}
