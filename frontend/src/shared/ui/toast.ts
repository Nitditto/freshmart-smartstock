export type ToastKind = 'success' | 'error' | 'info'

export interface ToastMessage {
  id: number
  kind: ToastKind
  text: string
}

const AUTO_DISMISS_MS = 5000

let messages: ToastMessage[] = []
let nextId = 1
const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

function push(kind: ToastKind, text: string) {
  const id = nextId++
  messages = [...messages, { id, kind, text }]
  emit()
  setTimeout(() => dismiss(id), AUTO_DISMISS_MS)
}

export function dismiss(id: number) {
  messages = messages.filter((m) => m.id !== id)
  emit()
}

/** A tiny store (not React state) so the axios interceptor can raise toasts from outside components. */
export const toast = {
  success: (text: string) => push('success', text),
  error: (text: string) => push('error', text),
  info: (text: string) => push('info', text),
}

export function subscribeToasts(listener: () => void) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getToasts() {
  return messages
}
