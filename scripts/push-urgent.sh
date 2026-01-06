#!/bin/bash

# Urgent Push Script - Bypasses quality checks
# Use only for emergency situations!

echo "URGENT PUSH MODE"
echo "Quality checks will be SKIPPED!"
echo ""

read -p "Are you sure you want to push without quality checks? (yes/no): " confirm

if [[ "$confirm" == "yes" ]] || [[ "$confirm" == "y" ]]; then
    echo "Pushing without quality checks..."
    SKIP_QUALITY_CHECKS=1 git push "$@"
    echo "Push completed (quality checks bypassed)"
else
    echo "Push cancelled"
    exit 1
fi