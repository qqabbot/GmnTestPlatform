---
description: QA Bug Report and Fix Workflow
---

# QA Bug Report and Fix Workflow

This workflow handles bug reports, creates GitHub issues, fixes bugs, and automatically closes issues after verification.

## Prerequisites

1. GitHub Personal Access Token with `repo` scope
2. Token should be set as environment variable: `export GITHUB_TOKEN=your_token_here`

## Workflow Steps

### 1. Receive Bug Report
- Bug can be reported manually by user
- Bug can be discovered by running verification scripts (verify_*.sh)

### 2. Create GitHub Issue
```bash
# Using GitHub API to create issue
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/qqabbot/GmnTestPlatform/issues \
  -d '{
    "title": "Bug: [Brief Description]",
    "body": "## Description\n[Detailed description]\n\n## Steps to Reproduce\n1. Step 1\n2. Step 2\n\n## Expected Behavior\n[What should happen]\n\n## Actual Behavior\n[What actually happens]\n\n## Environment\n- Backend: Running\n- Frontend: Running\n\n## Additional Context\n[Any additional information]",
    "labels": ["bug"]
  }'
```

### 3. Investigate and Fix
- Investigate the root cause
- Implement the fix
- Commit changes with issue reference: `git commit -m "fix: description (fixes #issue_number)"`

### 4. Verify Fix
- Run relevant verification scripts
- Test manually if needed
- Ensure the bug is resolved

### 5. Close Issue (Automatic)
```bash
# Close issue via API
curl -X PATCH \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/qqabbot/GmnTestPlatform/issues/{issue_number} \
  -d '{
    "state": "closed"
  }'

# Add verification comment
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/qqabbot/GmnTestPlatform/issues/{issue_number}/comments \
  -d '{
    "body": "✅ Fix verified and tested successfully.\n\nVerification steps:\n- [List what was tested]"
  }'
```

## QA Agent Responsibilities

1. **Listen** for bug reports from user or verification scripts
2. **Create** GitHub issue with proper labels and description
3. **Investigate** the root cause in the codebase
4. **Fix** the bug with proper code changes
5. **Verify** the fix works correctly
6. **Close** the issue automatically after successful verification
7. **Report** back to user with issue link and status
