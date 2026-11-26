import type { DocumentStatus, User } from '../../api/types'

/**
 * Mirrors the backend rule (DocumentGuard): only a DRAFT can change, and only by its creator or an ADMIN.
 * It only decides which buttons to show; the backend enforces it with 403 / 409.
 */
export function canModifyDocument(user: User | null, doc: { status: DocumentStatus; createdById: number }): boolean {
  if (!user || doc.status !== 'DRAFT') return false
  return user.role === 'ADMIN' || user.id === doc.createdById
}
