/** 默认每页条数；运行时会以服务端 `/api/public/config` 的 feedPageSize 为准 */
export const FEED_PAGE_SIZE_DEFAULT = 8

export const WATERFALL_LAYOUT = {
  itemMinWidth: 150,
  minColumnCount: 1,
  maxColumnCount: 4,
  gap: 8,
  padding: 6
}

/** 卡片封面默认高宽比（与 LazyImage cover 一致） */
export const CARD_COVER_RATIO_DEFAULT = 1.18

/** 卡片标题区占用高度（padding + 标题最多两行 + meta），供虚拟列表算高 */
export const CARD_BODY_EXTRA = 66

export const SKELETON_ITEMS = Array.from({ length: 12 }).map((_, index) => ({
  id: `skeleton-${index}`,
  ratio: 1 + ((index % 4) * 0.15)
}))
