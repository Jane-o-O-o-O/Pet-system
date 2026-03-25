export const roomPriceMap: Record<string, number> = {
  Standard: 30,
  Deluxe: 40,
  Suite: 50
}

export const roomTypeLabel = (type: string) => ({
  Standard: '标准间',
  Deluxe: '豪华间',
  Suite: '套房'
}[type] || type)
