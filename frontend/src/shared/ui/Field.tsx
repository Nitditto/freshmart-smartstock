import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { cn } from '../lib/cn'

const CONTROL =
  'w-full rounded-md border border-border bg-card px-3 py-2 text-sm text-foreground ' +
  'placeholder:text-muted-foreground disabled:bg-muted'

interface FieldShellProps {
  label: string
  error?: string
  hint?: string
  children: (id: string, describedBy: string | undefined) => ReactNode
  className?: string
}

/** Visible label + error right under the control (MASTER.md: forms), wired with aria. */
function FieldShell({ label, error, hint, children, className }: FieldShellProps) {
  const id = useId()
  const messageId = error || hint ? `${id}-msg` : undefined
  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <label htmlFor={id} className="text-sm font-medium text-foreground">
        {label}
      </label>
      {children(id, messageId)}
      {error ? (
        <p id={messageId} role="alert" className="text-xs text-danger">
          {error}
        </p>
      ) : hint ? (
        <p id={messageId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      ) : null}
    </div>
  )
}

interface CommonProps {
  label: string
  error?: string
  hint?: string
  containerClassName?: string
}

export const TextField = forwardRef<HTMLInputElement, CommonProps & InputHTMLAttributes<HTMLInputElement>>(
  function TextField({ label, error, hint, containerClassName, className, ...rest }, ref) {
    return (
      <FieldShell label={label} error={error} hint={hint} className={containerClassName}>
        {(id, describedBy) => (
          <input
            ref={ref}
            id={id}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            className={cn(CONTROL, error && 'border-danger', className)}
            {...rest}
          />
        )}
      </FieldShell>
    )
  },
)

export const SelectField = forwardRef<HTMLSelectElement, CommonProps & SelectHTMLAttributes<HTMLSelectElement>>(
  function SelectField({ label, error, hint, containerClassName, className, children, ...rest }, ref) {
    return (
      <FieldShell label={label} error={error} hint={hint} className={containerClassName}>
        {(id, describedBy) => (
          <select
            ref={ref}
            id={id}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            className={cn(CONTROL, error && 'border-danger', className)}
            {...rest}
          >
            {children}
          </select>
        )}
      </FieldShell>
    )
  },
)

export const TextAreaField = forwardRef<HTMLTextAreaElement, CommonProps & TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function TextAreaField({ label, error, hint, containerClassName, className, ...rest }, ref) {
    return (
      <FieldShell label={label} error={error} hint={hint} className={containerClassName}>
        {(id, describedBy) => (
          <textarea
            ref={ref}
            id={id}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            className={cn(CONTROL, error && 'border-danger', className)}
            {...rest}
          />
        )}
      </FieldShell>
    )
  },
)
