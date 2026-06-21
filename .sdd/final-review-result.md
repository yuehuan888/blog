## Verdict: NEEDS FIX

## Findings

### Important

1. **Lightbox Escape key won't work** — `pages/article/[id].vue:177`
   - The Teleport lightbox div has `@keydown.escape="closeLightbox"`, but a plain `<div>` is not focusable. Keyboard events never reach it.
   - Users cannot close the lightbox with the Escape key as expected. Only clicking outside/closing with the ✕ button works.
   - **Fix**: Add `tabindex="-1"` to the lightbox div and call `.focus()` when `lightboxVisible` becomes true (watch it in a `watchEffect` or in `openLightbox()`). Alternatively, use `document.addEventListener('keydown', ...)` in `onMounted` and remove it in `onUnmounted`.

2. **Missing Chinese error translation for article image size limit** — `api/index.ts:26`
   - The `uploadArticleImage` endpoint returns `"File size must be less than 5MB"` when images exceed 5MB.
   - The `ERROR_ZH` map only has the avatar variant `"File size must be less than 2MB": "文件大小不能超过 2MB"`.
   - The `translateError()` function will fall through all prefix/suffix matches and return the English raw string to the user.
   - **Fix**: Add `'File size must be less than 5MB': '文件大小不能超过 5MB'` to the `ERROR_ZH` map.

### Minor

3. **Masonry layout uses static height estimates** — `pages/index.vue:114-122`
   - `estimateCardHeight()` returns 250px for image cards and 120-165px for text cards, based on deterministic ID-based variation. Actual rendered heights depend on title length, tag count, and image aspect ratios, which may differ from the estimate.
   - Cards are positioned absolutely at estimated coordinates, so discrepancies create visual gaps or overlaps between cards.
   - This is acceptable for an MVP but will produce imperfect waterfall alignment for content-heavy cards.
   - **Improvement**: Use `nextTick` + `getBoundingClientRect()` after first render to measure actual heights and re-layout.

4. **PATCH endpoint ignores images** — `ArticleServiceImpl.java:338` (`patch()` method)
   - `updateById()` (PUT) correctly handles `entity.getImages()`, but `patch()` (PATCH) has no image handling logic. If a client sends images via PATCH, they are silently dropped.
   - The frontend `write.vue` uses `updateArticle()` (PUT), so this does not break the current feature.
   - **Fix**: Add image handling in `patch()` for API consistency, or document that images are only handled via PUT.

5. **Duplicate image query in `updateById`** — `ArticleServiceImpl.java:153,158`
   - `getById(entity.getId())` on line 153 calls the overridden `getById()` which queries `articleImageMapper.selectByArticleId()` to populate `article.images`.
   - Then line 158 calls `articleImageMapper.selectByArticleId(entity.getId())` again to get old images for deletion.
   - Two identical DB queries for the same data.
   - **Fix**: Reuse `old.getImages()` or the query result from `getById()` instead of re-querying.

6. **`onCarouselScroll` not throttled** — `pages/article/[id].vue:256-262`
   - The scroll event fires continuously (every frame) during scrolling. `Math.round(track.scrollLeft / slideWidth)` runs on each event.
   - Not noticeable for most users but uses unnecessary CPU.
   - **Improvement**: Wrap in `requestAnimationFrame` or apply a simple throttle.

## Summary

The article-images feature is well-implemented and functionally complete. The backend architecture (entity, mapper, service, scheduler, schema) is solid with correct cascade deletion, batch image loading to avoid N+1 queries, and safe orphan cleanup with 24-hour cutoff. The frontend covers all key flows: upload with preview, draft persistence, masonry layout, carousel with scroll-snap, and fullscreen lightbox. No hardcoded secrets, no `console.log` statements, and no commented-out code blocks were found. Two important issues need fixing before push: (1) the lightbox Escape key handler won't work because the div is not focusable, and (2) the 5MB article image error message lacks a Chinese translation in the ERROR_ZH map. Five minor issues are noted for future improvement but do not block push.
