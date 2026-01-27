# GitHub Actions & CI/CD

This repository uses GitHub Actions for continuous integration and deployment.

## Workflows

### CI Workflow (`.github/workflows/ci.yml`)
Runs on every push and pull request to `main` and `develop` branches.

**Jobs:**
- **Build**: Compiles debug and release APKs
- **Test**: Runs unit tests and instrumentation tests
- **Lint**: Performs Android lint checks
- **Check Format**: Validates code formatting
- **Security Scan**: Checks for security vulnerabilities

### PR Checks (`.github/workflows/pr-checks.yml`)
Runs on pull requests to perform pre-submit validation.

**Checks:**
- Build verification
- Unit test execution
- Test coverage validation
- TODO/FIXME detection
- Debug code detection
- File size validation
- AndroidManifest validation
- Hardcoded string detection

### Release Workflow (`.github/workflows/release.yml`)
Triggers when a version tag is pushed (e.g., `v1.0.0`).

**Actions:**
- Builds release APK
- Signs APK (if keystore configured)
- Creates GitHub release with APK

### CodeQL Analysis (`.github/workflows/codeql.yml`)
Performs security and quality analysis.

**Features:**
- Static code analysis
- Security vulnerability detection
- Code quality checks
- Runs weekly and on PRs

## Pre-commit Hooks

Install pre-commit hooks locally:

```bash
pip install pre-commit
pre-commit install
```

Hooks will run automatically on `git commit`:
- Trailing whitespace removal
- End of file fixes
- YAML/JSON validation
- Large file detection
- Merge conflict detection
- ktlint checks
- Android lint checks

## Dependabot

Automatically creates PRs for dependency updates:
- Gradle dependencies (weekly)
- GitHub Actions (weekly)

## Status Badges

Add these badges to your README:

```markdown
![CI](https://github.com/YOUR_USERNAME/HeadsetUsageTracker/workflows/CI/badge.svg)
![PR Checks](https://github.com/YOUR_USERNAME/HeadsetUsageTracker/workflows/PR%20Checks/badge.svg)
![CodeQL](https://github.com/YOUR_USERNAME/HeadsetUsageTracker/workflows/CodeQL%20Analysis/badge.svg)
```

## Secrets

Configure these secrets in GitHub Settings → Secrets:

- `KEYSTORE_BASE64`: Base64 encoded keystore file (for signing)
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

## Local Testing

Test workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or download from https://github.com/nektos/act/releases

# Run CI workflow
act -j build
act -j test
```

## Troubleshooting

### Build Failures
- Check Android SDK setup
- Verify Gradle cache
- Review build logs

### Test Failures
- Ensure all tests pass locally
- Check test coverage
- Review test logs

### Lint Failures
- Run `./gradlew lintDebug` locally
- Fix reported issues
- Review lint report


