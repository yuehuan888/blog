## Status: DONE

## Commits
- 68f6af7 feat: add article_image table for article image attachments

## Test Results
- tail -15 blog-backend/src/main/resources/schema.sql: confirmed article_image table DDL present at end of file with all columns (id, article_id, url, sort_order, created_at) and idx_article_id index

## Concerns
- NONE
