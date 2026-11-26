import { describe, expect, it } from 'vitest'
import type { User } from '../../api/types'
import { canModifyDocument } from './permissions'

const staff: User = { id: 2, fullName: 'Staff', email: 's@x', role: 'STAFF', active: true, createdAt: '' }
const admin: User = { ...staff, id: 1, role: 'ADMIN' }

describe('canModifyDocument', () => {
  it('lets a STAFF member change only their own DRAFT', () => {
    expect(canModifyDocument(staff, { status: 'DRAFT', createdById: 2 })).toBe(true)
    expect(canModifyDocument(staff, { status: 'DRAFT', createdById: 3 })).toBe(false)
  })

  it('lets ADMIN change any DRAFT', () => {
    expect(canModifyDocument(admin, { status: 'DRAFT', createdById: 3 })).toBe(true)
  })

  it('never offers actions on a CONFIRMED or CANCELLED document', () => {
    expect(canModifyDocument(admin, { status: 'CONFIRMED', createdById: 1 })).toBe(false)
    expect(canModifyDocument(admin, { status: 'CANCELLED', createdById: 1 })).toBe(false)
  })

  it('offers nothing to an anonymous visitor', () => {
    expect(canModifyDocument(null, { status: 'DRAFT', createdById: 1 })).toBe(false)
  })
})
