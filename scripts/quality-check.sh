#!/bin/bash

# Quality Check Script
# Run this before pushing or when you want to check code quality

echo "Running comprehensive quality checks..."
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

PASSED="${GREEN}PASSED${NC}"
FAILED="${RED}FAILED${NC}"

# Function to run a check
run_check() {
    local name="$1"
    local command="$2"
    echo -n "Running $name... "

    if eval "$command" > /dev/null 2>&1; then
        echo -e "$PASSED"
        return 0
    else
        echo -e "$FAILED"
        return 1
    fi
}

FAILED_CHECKS=0

# Run all checks
echo "1. Unit Tests"
run_check "Unit Tests" "mvn test -q -Dspotbugs.skip=true -Dpmd.skip=true" || ((FAILED_CHECKS++))

echo ""
echo "2. Code Quality Checks"
run_check "SpotBugs (Bug Detection)" "mvn spotbugs:check -q" || ((FAILED_CHECKS++))
run_check "PMD (Code Quality)" "mvn pmd:check -q" || ((FAILED_CHECKS++))

echo ""
echo "3. Build Check"
run_check "Clean Build" "mvn clean compile -q" || ((FAILED_CHECKS++))

# Summary
echo ""
echo "========================================"
if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}All quality checks passed!${NC}"
    echo ""
    echo "Safe to push your changes."
    exit 0
else
    echo -e "${RED}$FAILED_CHECKS check(s) failed!${NC}"
    echo ""
    echo "Fix the issues or bypass checks with:"
    echo "   export SKIP_QUALITY_CHECKS=1"
    echo "   git push"
    echo ""
    echo "Or run individual checks:"
    echo "   mvn test                    # Run only tests"
    echo "   mvn spotbugs:check         # Run only SpotBugs"
    echo "   mvn pmd:check              # Run only PMD"
    exit 1
fi