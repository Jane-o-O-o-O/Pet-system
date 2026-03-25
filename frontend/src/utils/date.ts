const pad = (value: number) => String(value).padStart(2, '0')

const parseDateParts = (value: string) => {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return null
  const [, y, m, d] = match
  return {
    year: Number(y),
    month: Number(m),
    day: Number(d)
  }
}

const parseDateTimeParts = (value: string) => {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?/)
  if (!match) return null
  const [, y, m, d, hh, mm, ss] = match
  return {
    year: Number(y),
    month: Number(m),
    day: Number(d),
    hour: Number(hh),
    minute: Number(mm),
    second: Number(ss || '0')
  }
}

export const formatDate = (value?: string | null) => {
  if (!value) return '-'
  const dateOnly = parseDateParts(value)
  if (dateOnly) {
    return `${dateOnly.year}-${pad(dateOnly.month)}-${pad(dateOnly.day)}`
  }

  const dateTime = parseDateTimeParts(value)
  if (dateTime) {
    return `${dateTime.year}-${pad(dateTime.month)}-${pad(dateTime.day)}`
  }

  const nativeDate = new Date(value)
  if (Number.isNaN(nativeDate.getTime())) return value
  return `${nativeDate.getFullYear()}-${pad(nativeDate.getMonth() + 1)}-${pad(nativeDate.getDate())}`
}

export const formatDateTime = (value?: string | null) => {
  if (!value) return '-'
  const parsed = parseDateTimeParts(value)
  if (parsed) {
    return `${parsed.year}-${pad(parsed.month)}-${pad(parsed.day)} ${pad(parsed.hour)}:${pad(parsed.minute)}`
  }

  const nativeDate = new Date(value)
  if (Number.isNaN(nativeDate.getTime())) return value
  return `${nativeDate.getFullYear()}-${pad(nativeDate.getMonth() + 1)}-${pad(nativeDate.getDate())} ${pad(nativeDate.getHours())}:${pad(nativeDate.getMinutes())}`
}

export const daysBetweenDates = (start: string, end: string) => {
  const startParts = parseDateParts(start)
  const endParts = parseDateParts(end)
  if (!startParts || !endParts) return 0

  const startDate = new Date(startParts.year, startParts.month - 1, startParts.day)
  const endDate = new Date(endParts.year, endParts.month - 1, endParts.day)
  return Math.max(1, Math.ceil((endDate.getTime() - startDate.getTime()) / 86400000))
}

export const todayText = () => {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
