/** Shared Tailwind class strings so every table looks the same (dense dashboard style). */
export const TABLE = 'w-full text-left text-sm'
export const TH = 'bg-primary px-3 py-2 text-xs font-semibold tracking-wide text-white uppercase whitespace-nowrap'
export const TD = 'border-t border-border px-3 py-2 align-middle'
export const TD_NUM = `${TD} text-right tabular-nums`
export const TH_NUM = `${TH} text-right`
/** SKU and document codes must never wrap. */
export const TD_CODE = `${TD} whitespace-nowrap`
