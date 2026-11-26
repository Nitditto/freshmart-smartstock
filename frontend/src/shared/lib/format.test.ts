import { describe, expect, it } from 'vitest'
import { businessDate, firstDayOfMonth, formatDateTime, formatNumber, formatVnd } from './format'

describe('format', () => {
  it('formats money in VND without decimals', () => {
    expect(formatVnd(26000).replace(/\s/g, ' ')).toBe('26.000 ₫')
    expect(formatVnd(null)).toBe('-')
  })

  it('formats plain numbers with Vietnamese grouping', () => {
    expect(formatNumber(1250000)).toBe('1.250.000')
    expect(formatNumber(undefined)).toBe('-')
  })

  it('shows UTC instants in the business time zone (UTC+7)', () => {
    // 17:00 UTC on 20/11 is already 00:00 on 21/11 in Ho Chi Minh City.
    expect(formatDateTime('2026-11-20T17:00:00Z')).toContain('21/11/2026')
    expect(formatDateTime(null)).toBe('-')
  })

  it('uses the business calendar day, not the UTC day', () => {
    expect(businessDate(new Date('2026-11-20T17:30:00Z'))).toBe('2026-11-21')
  })

  it('starts a report range on the first day of the business month', () => {
    expect(firstDayOfMonth(new Date('2026-11-26T05:00:00Z'))).toBe('2026-11-01')
  })
})
