#!/bin/bash

# Script to run tests with coverage using Docker

set -e

echo "🧪 Building test Docker image..."
docker build -t url-shortener-test -f Dockerfile.test .

echo "🚀 Running tests with coverage..."
docker run --rm url-shortener-test

echo "📊 Extracting coverage report..."
CONTAINER_ID=$(docker create url-shortener-test)
docker cp $CONTAINER_ID:/app/target/scala-3.6.2/scoverage-report ./coverage-report
docker rm $CONTAINER_ID

echo "✅ Coverage report generated at: ./coverage-report/index.html"
echo "📈 Opening coverage report..."

# Open the report in browser (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    open coverage-report/index.html
else
    echo "Please open coverage-report/index.html in your browser"
fi
