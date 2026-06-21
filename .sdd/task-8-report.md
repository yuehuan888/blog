# Task 8 Report: Add Image Upload Area to Write Page

## Status: Complete

## Changes Made

### File Modified
- `blog-frontend/pages/article/write.vue`

### Template Changes
- Added image upload area between the title field and tags field
- Thumbnail grid showing uploaded images (80x80 rounded squares)
- Each thumbnail shows a delete button (X) on hover
- First image labeled "封面" (cover), others numbered 2-9
- NSpin spinner overlay during upload
- Dashed "+" add button (hidden when 9 images reached)
- Hidden file input (accept="image/*", multiple)

### Script Changes
- Added `NSpin` to Naive UI imports
- Added `uploadArticleImage` to API imports
- Added reactive state: `uploadedImages` (string[]), `uploadingStates` (boolean[]), `imageInputRef`
- `triggerImageInput()`: opens file picker via ref
- `handleImageSelect()`: validates count (max 9) and size (max 5MB), uploads immediately, shows local blob preview while uploading, replaces with server URL on success, removes on failure
- `removeImage(idx)`: removes image and its uploading state by index
- `getRelativeUrls()`: converts absolute/local URLs to relative paths for API submission
- `saveArticle()`: checks for still-uploading images before saving, includes `images` in create/update payloads
- `saveDraftToLocal()`: persists `images` (excluding blob URLs) in localStorage
- `loadDraft()`: restores `images` from localStorage draft
- `fetchArticleForEdit()`: restores images from server `article.images`, prefixing relative URLs with API base

### Typecheck
- Passed after fixing `NSpin size="tiny"` to `size="small"` (Naive UI type constraint)

## Commit
- `8e8277d` feat: add image upload area to write page
