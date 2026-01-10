# Playwright E2E Tests

## Test Coverage

### Batch 1: Home Page Basic Functionality (4 tests)
- ✅ Home page loads successfully
- ✅ All feature cards are displayed
- ✅ Navigation links are visible
- ✅ Statistics section is displayed

### Batch 2: Theme Toggle Functionality (4 tests)
- ✅ Toggle between light and dark themes
- ✅ Theme preference persists after reload
- ✅ Correct theme icon is displayed
- ✅ Theme tooltip shows on hover

### Batch 3: Responsive Design (4 tests)
- ✅ Mobile menu displays on small screens
- ✅ Feature cards stack on mobile
- ✅ Tablet layout displays properly
- ✅ Desktop layout displays properly

### Total Tests: 12

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
- `theme.spec.ts` - Theme toggle tests
- `responsive.spec.ts` - Responsive design tests


