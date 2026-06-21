## Spec Compliance: ✅

All 7 spec requirements verified:

1. **Transient fields** — Article.java lines 40-47: `coverImage`, `images`, `imageCount` all with `@TableField(exist = false)`.
2. **getById populates images** — Lines 111-122: queries `articleImageMapper.selectByArticleId`, sets `images` list, `coverImage` from first image, `imageCount` from size. Empty case handled with `Collections.emptyList()` and `0`.
3. **save persists images** — Lines 134-141: loops with index `i` as `sortOrder`, inserts each `ArticleImage`.
4. **updateById replaces images** — Lines 156-176: deletes old image files from disk (try-catch), deletes old DB records, inserts new ones. Only triggers when `entity.getImages() != null` (null = preserve, empty list = clear all).
5. **removeById cascade (step 6.5)** — Lines 234-244: cleans up image files (try-catch) then DB records, positioned between history cleanup (step 6) and article deletion (step 7). All within `@Transactional`.
6. **page batch-loads** — Both tagId path (lines 282-293) and normal path (lines 321-332) have identical batch-loading via `batchCoverMap`/`batchCountMap`.
7. **File deletion path** — `new File(uploadDir, url.replace("/uploads/", ""))` used consistently in updateById (line 161) and removeById (line 238).

## Code Quality: Approved

- **Compilation**: `mvn compile` passes with BUILD SUCCESS.
- **Imports**: `File`, `Collections`, `HashMap`, `Collectors`, `List`, `ArticleImage`, `ArticleImageMapper`, `Value` — all verified used. No unused imports.
- **Additive change** — No existing logic modified. Image handling is entirely new code injected into overridden methods.
- **Helper methods** — `batchCoverMap` and `batchCountMap` are `private`, used in both page code paths.
- **File deletion safety** — Both call sites wrap file deletion in try-catch, logging `log.warn` on failure. Missing files (already deleted or never existed) are silently skipped.

### Findings

**No critical, important, or minor issues found.**
