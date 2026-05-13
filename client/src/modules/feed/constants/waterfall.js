/** 默认每页条数；运行时会以服务端 `/api/public/config` 的 feedPageSize 为准 */
export const FEED_PAGE_SIZE_DEFAULT = 8

export const WATERFALL_LAYOUT = {
  itemMinWidth: 150,
  minColumnCount: 1,
  maxColumnCount: 4,
  gap: 8,
  padding: 6
}

export const SKELETON_ITEMS = Array.from({ length: 12 }).map((_, index) => ({
  id: `skeleton-${index}`,
  ratio: 1 + ((index % 4) * 0.15)
}))
