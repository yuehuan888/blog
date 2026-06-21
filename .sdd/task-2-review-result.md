## Spec Compliance: ✅

All requirements are met:

- **ArticleImage entity** — 5 fields present: `id` (auto-increment), `articleId`, `url`, `sortOrder`, `createdAt` (auto-fill on insert)
- **Mapper extends BaseMapper<ArticleImage>** — inherits standard CRUD
- **deleteByArticleId** — `@Delete` annotation with correct SQL
- **selectByArticleId** — `@Select` with `ORDER BY sort_order`
- **countByArticleIds** — dynamic SQL with `<foreach>`, returns `article_id` + `cnt` grouped by `article_id`
- **selectCoverImages** — dynamic SQL filtering `sort_order = 0`

## Code Quality: Approved

### Findings

**Minor**
- `@Mapper` import style differs from some existing mappers. `ArticleImageMapper` imports `org.apache.ibatis.annotations.Mapper` and uses `@Mapper`, while `ArticleLikeMapper` (and others) use the fully qualified form `@org.apache.ibatis.annotations.Mapper` without importing it. Both styles are valid and functional — this is purely a consistency nitpick. The approach used in the new file is arguably cleaner and more common, so no change is required.

### Why Approved

- **Package names**: `com.blog.entity` and `com.blog.mapper` — match existing conventions exactly
- **Entity structure**: Mirrors `ArticleLike` entity pattern — same annotation order, same field layout, same `@TableId(type = IdType.AUTO)` + `@TableField(fill = FieldFill.INSERT)` combo
- **No unused imports**: Both files have exactly the imports they need
- **Mapper follows project patterns**: `@Mapper` on the interface, extends `BaseMapper`, custom methods as annotation-based SQL, `<script>` + `<foreach>` for IN-clause dynamic SQL
- **Naming conventions**: Method names (`deleteByArticleId`, `selectByArticleId`, `countByArticleIds`, `selectCoverImages`) are clear and self-documenting
- **Type choices**: `Long` for IDs, `Integer` for sortOrder, `List<Map<String, Object>>` for the aggregate query — all appropriate
