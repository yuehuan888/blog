# Task 7 Report: ArticleCard Redesign with Cover Images

**Status**: Completed
**Date**: 2026-06-21

## Summary

Rewrote `blog-frontend/components/article/ArticleCard.vue` to display article cover images with a multi-image count badge, while preserving the emoji fallback for articles without images.

## Changes Made

### File Modified
- `blog-frontend/components/article/ArticleCard.vue` — full rewrite

### Key Changes from Previous Version

1. **Cover Image Display**: When `article.coverImage` is present and hasn't failed to load, an `<img>` tag is rendered with `object-cover`, lazy loading, and responsive height constraints (min 120px, max 320px). The image area has a `bg-gray-100` placeholder background while loading.

2. **Multi-Image Count Badge**: When `article.imageCount > 1`, a `+N` badge (where N = imageCount - 1) is displayed on the bottom-right of the cover image. Styled with `bg-black/60` semi-transparent background, white text, small rounded corners.

3. **Image URL Resolution**: New `imageUrl()` helper prepends `http://localhost:8080` to relative paths (those not starting with `http://` or `https://`), matching the existing pattern in `UserAvatar.vue`.

4. **Image Error Handling**: `imgFailed` ref is set to `true` on `@error`, which hides the `<img>` and shows the emoji fallback. A `watch` on `props.article.coverImage` resets `imgFailed` to `false` so that if the article data updates with a new cover image, it retries.

5. **Emoji Fallback Preserved**: Articles without `coverImage` (or with failed image loads) still show the deterministic emoji placeholder. The fallback height was reduced from `h-32` (128px) to `h-24` (96px) and text size from `text-4xl` to `text-3xl` to differentiate from the larger image area.

6. **Card Clickability**: Added `cursor-pointer` to the NCard wrapper for better UX feedback.

7. **Meta Spacing**: Added `mt-1` to the meta row for consistent spacing when the author row is absent.

## Verification

- `npx nuxi typecheck` passed with no errors
- TypeScript types (`coverImage: string | null`, `imageCount: number`) were already defined in `blog-frontend/types/index.ts` from Task 6
