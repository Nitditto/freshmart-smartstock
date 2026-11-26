import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ShortageItem } from '../../api/types'
import { ShortageDialog } from './ShortageDialog'

const items: ShortageItem[] = [
  { productId: 5, sku: 'RCQ-005', name: 'Chuối', requested: 100, available: 30 },
  { productId: 8, sku: 'TCA-002', name: 'Ức gà', requested: 50, available: 40 },
]

describe('ShortageDialog', () => {
  it('lists every short product with what was asked, what exists and the gap', () => {
    render(<ShortageDialog items={items} onClose={() => {}} />)

    expect(screen.getByText(/Không đủ tồn kho để xuất/)).toBeInTheDocument()
    const chuoi = screen.getByText('Chuối').closest('tr')!
    expect(chuoi).toHaveTextContent('100')
    expect(chuoi).toHaveTextContent('30')
    expect(chuoi).toHaveTextContent('70')
    expect(screen.getByText('Ức gà').closest('tr')).toHaveTextContent('10')
  })

  it('tells the user that nothing was deducted', () => {
    render(<ShortageDialog items={items} onClose={() => {}} />)

    expect(screen.getByText(/không bị trừ/)).toBeInTheDocument()
  })

  it('renders nothing while there is no shortage to show', () => {
    render(<ShortageDialog items={null} onClose={() => {}} />)

    expect(screen.queryByText('Chuối')).not.toBeInTheDocument()
  })
})
