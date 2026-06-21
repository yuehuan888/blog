## Spec Compliance: ❌

**Critical issue**: `spring.servlet.multipart.max-file-size: 2MB` in `application.yml` (line 43) will reject files > 2MB at the servlet level **before** they reach the controller. The controller's 5MB check (`file.getSize() > 5 * 1024 * 1024`) is unreachable for files between 2MB and 5MB — Spring's `MultipartResolver` throws `MultipartException` first.

**Fix required**: Change `max-file-size` and `max-request-size` to at least `5MB`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

### Individual checks:

| Check | Status | Notes |
|-------|--------|-------|
| Endpoint path: POST /api/upload/article-image | ✅ | `@PostMapping("/upload/article-image")` on `@RequestMapping("/api")` |
| 5MB size check (5 * 1024 * 1024) | ✅ | Code correct, but blocked by servlet config (see above) |
| image/* MIME check | ✅ | `contentType.startsWith("image/")` |
| Stores to uploads/articles/ directory | ✅ | `Paths.get(uploadDir, "articles")` |
| Filename prefix is "article_" | ✅ | `"article_" + UUID...substring(0, 8) + ext` |
| Returns { url: "/uploads/articles/..." } | ✅ | `Map.of("url", "/uploads/articles/" + filename)` |
| Auth whitelist covers this | ✅ | `excludePathPatterns("/api/upload/**")` in `WebMvcConfig.java:30` already matches |

## Code Quality: Needs Fix

**Good**:
- Follows the exact same validation order and error message pattern as `uploadAvatar` — consistency is well maintained.
- Uses only existing imports; no new dependencies.
- Proper `try-catch (IOException e)` wrapping the file I/O.
- Clean, minimal addition — no unnecessary abstractions.

**Issue**:
- The servlet-level multipart size limit (`2MB`) must be bumped to at least `5MB` to make the controller's 5MB gate functional. Without this change, article images sized 2–5 MB will be rejected with a Spring-level error rather than the controller's user-friendly `"File size must be less than 5MB"` message.
