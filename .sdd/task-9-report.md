# Task 9 Report: Homepage Masonry Waterfall Layout

## Summary

Replaced the fixed CSS grid layout (`grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4`) on the homepage with a JS-powered masonry waterfall layout.

## Changes Made

### File: `blog-frontend/pages/index.vue`

**Template Changes:**

1. **Skeleton loading** — Replaced grid with flex wrap using `columnCount` for responsive width calculation and variable heights (`140 + (i % 3) * 40` px) for visual interest.

2. **Article cards** — Replaced grid with:
   - Relative container (`masonryRef`) with dynamic height
   - Absolutely positioned child divs with smooth CSS transitions

**Script Additions:**

- **State**: `masonryRef`, `columnCount` (ref, default 3), `gap` (const 16), `containerHeight` (ref), `cardPositions` (typed array)
- **`updateColumnCount()`** — Sets column count by breakpoint (2/3/4), triggers relayout on change
- **`getColumnWidth()`** — Calculates column width from container + gap accounting
- **`estimateCardHeight()`** — Estimates card height (120px base, 250px for image cards, +0-30px deterministic variation by article id)
- **`layoutCards()`** — Waterfall algorithm: places each card in the shortest column, stores positions, updates container height
- **`getCardStyle(index)`** — Template helper that returns pre-calculated position/style
- **`handleResize()`** — Delegates to `updateColumnCount()`

**Watcher Updates:**
- `watch([articles.length, activeTagId])` — now calls `layoutCards()` + `setupObserver()`
- `watch(activeTagId)` — chains `layoutCards()` after `fetchArticles(1)`

**Lifecycle Updates:**
- `onMounted` — calls `updateColumnCount()`, chains `layoutCards()` after initial fetch, adds `resize` listener
- `onUnmounted` — removes `resize` listener

## Verification

- `npx nuxi typecheck` — passed with no errors
- Committed to branch `worktree-article-images` as commit `f27052c`

## Known Considerations

- Card heights are estimated (not measured from DOM). Real rendered heights may differ slightly from estimates, but the deterministic variation and short-column algorithm keep the layout visually balanced.
- The `transition-all duration-300` class provides smooth repositioning when cards are appended via infinite scroll.
