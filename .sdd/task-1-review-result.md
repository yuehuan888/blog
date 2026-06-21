# Task 1 Review: `article_image` Table Schema

## Spec Compliance: ✅

| Requirement | Status | Evidence |
|---|---|---|
| Table name: `article_image` | ✅ | Line 114 |
| PK `id` BIGINT AUTO_INCREMENT | ✅ | Line 115 |
| `article_id` BIGINT NOT NULL | ✅ | Line 116 |
| `url` VARCHAR(500) NOT NULL | ✅ | Line 117 — matches spec constraint of 500 chars for relative paths |
| `sort_order` INT NOT NULL DEFAULT 0 | ✅ | Line 118 — 0 = cover image per spec |
| `created_at` DATETIME NOT NULL | ✅ | Line 119 |
| INDEX on `article_id` | ✅ | Line 120: `INDEX idx_article_id (article_id)` |
| Uses `CREATE TABLE IF NOT EXISTS` | ✅ | Line 114 — matches every table in the existing file |

## Code Quality: Approved

### What was checked

- **SQL style consistency**: Keywords are uppercase (`CREATE TABLE`, `BIGINT`, `NOT NULL`, `DEFAULT`, `INDEX`), names are lowercase (`article_image`, `id`, `article_id`, `url`, `sort_order`, `created_at`, `idx_article_id`). Matches all 10 existing tables in `schema.sql`.
- **Indentation**: 4-space column indent, consistent with every existing table definition. Internal column alignment is self-consistent within the table block (same column count from line start to type keyword across all 5 columns).
- **Index naming**: `idx_article_id` follows the `idx_<column>` convention used by `idx_follower`, `idx_following`, `idx_created_at`, `idx_article_created`, `idx_article_version`, `idx_article_parent`, `idx_parent` in the existing file.
- **Placement**: Appended after `user_follow` (the previous last table), keeping chronological/semantic adjacency (references `article` table, near other `article_*` join tables).
- **No extraneous columns or constraints**: Exactly the columns defined in the spec, nothing more.

### Findings

None. The change is a clean, minimal addition that follows all existing conventions precisely.

### Context note

The `article_image` table now makes the schema total 12 tables. The `url` column's `VARCHAR(500)` aligns with the same limit used for `user.avatar`. No foreign key constraint is declared inline (or on any table in this schema.sql) — this is consistent with the project's approach of enforcing referential integrity at the application layer.
