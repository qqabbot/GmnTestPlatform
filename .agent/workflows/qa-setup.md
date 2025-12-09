---
description: Setup GitHub API access for QA workflow
---

# QA Workflow Setup

## One-Time Setup

### 1. Create GitHub Personal Access Token

1. Go to GitHub: https://github.com/settings/tokens
2. Click "Generate new token" → "Generate new token (classic)"
3. Give it a name: "GmnTestPlatform QA Bot"
4. Select scopes:
   - ✅ `repo` (Full control of private repositories)
5. Click "Generate token"
6. **Copy the token immediately** (you won't see it again)

### 2. Set Environment Variable

Add to your `~/.zshrc`:

```bash
export GITHUB_TOKEN="your_personal_access_token_here"
```

Then reload:
```bash
source ~/.zshrc
```

### 3. Verify Setup

```bash
# Test API access
curl -H "Authorization: token $GITHUB_TOKEN" \
  https://api.github.com/repos/qqabbot/GmnTestPlatform
```

If successful, you'll see repository information in JSON format.

## Alternative: Install GitHub CLI (Optional)

```bash
brew install gh
gh auth login
```

This provides a more user-friendly interface but API access is sufficient for automation.