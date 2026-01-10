# Playwright E2E Tests

## Test Coverage

### Batch 1: Home Page Basic Functionality (4 tests)
- ✅ Home page loads successfully
- ✅ All feature cards are displayed
- ✅ Navigation links are visible
- ✅ Statistics section is displayed

### Total Tests: 4

## Running Tests

```bash
# Run all tests
npm run test:e2e

# Run tests in UI mode
npm run test:e2e:ui

# Run tests in headed mode (see browser)
npm run test:e2e:headed

# Debug tests
npm run test:e2e:debug
```

## Test Structure

Tests are organized in the `e2e/` directory:
- `home.spec.ts` - Home page tests
