# Task 10 Report: Image Carousel with Lightbox on Article Detail Page

**Status:** Completed
**Date:** 2026-06-20

## Changes Made

### File: `blog-frontend/pages/article/[id].vue`

#### 1. Template: Image Carousel (between Stats Bar and Tags)
- Added a horizontal scroll-snap carousel showing `article.images` at 360px height
- Each image fills the full carousel width via `w-full snap-center`
- Prev/Next navigation buttons appear conditionally (not on first/last slide)
- Dot indicators show current position, highlight active dot
- Clicking any image opens the lightbox

#### 2. Template: Lightbox (Teleport to body, before `</template>`)
- Fullscreen black overlay (`bg-black/90`, `z-50`)
- Close button (top-right), prev/next navigation (left/right edges)
- Image counter ("N / M") at bottom center
- Click background or close button to dismiss; `@keydown.escape` also closes
- `@click.stop` on the image prevents background-click dismissal

#### 3. Script: Icon Imports
- Added `ChevronBackOutline` and `ChevronForwardOutline` to `@vicons/ionicons5` import

#### 4. Script: Carousel State & Logic
- `carouselTrack` ref to the scroll container
- `currentSlide` computed from `scrollLeft / clientWidth` on scroll event
- `carouselImageUrl(src)` — prepends `http://localhost:8080` for relative paths
- `slideTo(index)` — smooth-scrolls the track to the given slide

#### 5. Script: Lightbox State & Logic
- `lightboxVisible`, `lightboxIndex` refs
- `openLightbox(index)` — sets index and shows lightbox
- `closeLightbox()` — hides lightbox

#### 6. Style
- Added `<style scoped>` block with `.carousel-track::-webkit-scrollbar { display: none; }`
- Carousel track also uses inline `scrollbar-width: none` and `-ms-overflow-style: none` for Firefox/IE

## Verification

- `npx nuxi typecheck` — **passed** (exit code 0)
- All types resolve correctly (`article.images` is typed as `string[]`)
- Carousel is conditionally rendered: only shown when `article.images.length > 0`
- Lightbox uses optional chaining (`article?.images?.[lightboxIndex]`) for safety
