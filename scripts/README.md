# Scripts

This folder contains utility scripts for development workflow and quality assurance.

## Available Scripts

### `quality-check.sh`
**Purpose**: Comprehensive code quality check before pushing changes

**What it does**:
- Runs unit tests
- Checks for bugs with SpotBugs
- Validates code quality with PMD
- Verifies clean compilation

**Usage**:
```bash
./scripts/quality-check.sh
```

**Output**:
```
Running comprehensive quality checks...
========================================
1. Unit Tests
Running Unit Tests... PASSED

2. Code Quality Checks
Running SpotBugs (Bug Detection)... PASSED
Running PMD (Code Quality)... PASSED

3. Build Check
Running Clean Build... PASSED

========================================
All quality checks passed!

Safe to push your changes.
```

### `push-urgent.sh`
**Purpose**: Emergency push bypass with confirmation

**When to use**: Only for urgent situations when you need to push without quality checks

**What it does**:
- Prompts for confirmation before proceeding
- Sets `SKIP_QUALITY_CHECKS=1` environment variable
- Executes `git push` with bypassed checks

**Usage**:
```bash
./scripts/push-urgent.sh
```

**Example**:
```
URGENT PUSH MODE
Quality checks will be SKIPPED!

Are you sure you want to push without quality checks? (yes/no): yes
Pushing without quality checks...
Push completed (quality checks bypassed)
```

## Quick Bypass (Without Script)

If you need to bypass checks quickly without the confirmation prompt:

```bash
SKIP_QUALITY_CHECKS=1 git push
```

## Automatic Quality Checks

The pre-push git hook automatically runs quality checks before every push. These scripts provide manual control when needed.