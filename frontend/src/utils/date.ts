const pad = (value: number) => String(value).padStart(2, '0')

type DateValue = string | number | Date | number[] | null | undefined
type ParsedDateParts = {
  year: number
  month: number
  day: number
}
type ParsedDateTimeParts = ParsedDateParts & {
  hour: number
  minute: number
  second: number
}

const stringifyDateValue = (value: DateValue) => {
  if (Array.isArray(value)) return value.join('-')
  return String(value)
}

const parseArrayDateParts = (value: DateValue): ParsedDateTimeParts | null => {
  if (!Array.isArray(value) || value.length < 3) return null

  const parts = value.map(item => Number(item))
  const year = parts[0]!
  const month = parts[1]!
  const day = parts[2]!
  const hour = parts[3] ?? 0
  const minute = parts[4] ?? 0
  const second = parts[5] ?? 0
  if ([year, month, day, hour, minute, second].some(item => Number.isNaN(item))) return null

  return { year, month, day, hour, minute, second }
}

const parseDateParts = (value: DateValue): ParsedDateParts | null => {
  const arrayParts = parseArrayDateParts(value)
  if (arrayParts) {
    return {
      year: arrayParts.year,
      month: arrayParts.month,
      day: arrayParts.day
    }
  }

  if (typeof value !== 'string') return null

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return null
  const [, y, m, d] = match
  return {
    year: Number(y),
    month: Number(m),
    day: Number(d)
  }
}

const parseDateTimeParts = (value: DateValue): ParsedDateTimeParts | null => {
  const arrayParts = parseArrayDateParts(value)
  if (arrayParts) return arrayParts

  if (typeof value !== 'string') return null

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

export const formatDate = (value?: DateValue) => {
  if (!value) return '-'
  const dateOnly = parseDateParts(value)
  if (dateOnly) {
    return `${dateOnly.year}-${pad(dateOnly.month)}-${pad(dateOnly.day)}`
  }

  const dateTime = parseDateTimeParts(value)
  if (dateTime) {
    return `${dateTime.year}-${pad(dateTime.month)}-${pad(dateTime.day)}`
  }

  const nativeDate = value instanceof Date ? value : new Date(value as string | number)
  if (Number.isNaN(nativeDate.getTime())) return stringifyDateValue(value)
  return `${nativeDate.getFullYear()}-${pad(nativeDate.getMonth() + 1)}-${pad(nativeDate.getDate())}`
}

export const formatDateTime = (value?: DateValue) => {
  if (!value) return '-'
  const parsed = parseDateTimeParts(value)
  if (parsed) {
    return `${parsed.year}-${pad(parsed.month)}-${pad(parsed.day)} ${pad(parsed.hour)}:${pad(parsed.minute)}`
  }

  const nativeDate = value instanceof Date ? value : new Date(value as string | number)
  if (Number.isNaN(nativeDate.getTime())) return stringifyDateValue(value)
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
