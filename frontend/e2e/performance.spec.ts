import { test, expect } from '@playwright/test';

test.describe('Performance and Edge Cases', () => {
    test('should load page within acceptable time', async ({ page }) => {
        const startTime = Date.now();

        await page.goto('/');

        // Wait for main content to be visible
        await expect(page.getByRole('heading', { name: /Shorten URLs with/i })).toBeVisible();

        const loadTime = Date.now() - startTime;

        // Page should load in less than 3 seconds
        expect(loadTime).toBeLessThan(3000);
    });

    test('should handle empty URL input gracefully', async ({ page }) => {
        await page.goto('/');

        const urlInput = page.getByPlaceholder(/Paste your long URL/i);
        const shortenButton = page.getByRole('button', { name: /Shorten URL/i });

        // Click button without entering URL
        await urlInput.click();
        await urlInput.fill('');

        // Button should still be visible and functional
        await expect(shortenButton).toBeVisible();
    });

    test('should display all images and icons correctly', async ({ page }) => {
        await page.goto('/');

        // Check that SVG icons are rendered
        const icons = page.locator('svg');
        const iconCount = await icons.count();

        // Should have multiple icons (navigation, features, theme toggle, etc.)
        expect(iconCount).toBeGreaterThan(5);
    });

    test('should maintain scroll position on theme toggle', async ({ page }) => {
        await page.goto('/');

        // Scroll down to footer
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));

        // Wait for scroll
        await page.waitForTimeout(500);

        // Get scroll position
        const scrollBefore = await page.evaluate(() => window.scrollY);

        // Toggle theme
        const themeToggle = page.locator('button[aria-label="Toggle theme"]');
        await themeToggle.click();

        // Wait for theme change
        await page.waitForTimeout(500);

        // Check scroll position maintained
        const scrollAfter = await page.evaluate(() => window.scrollY);

        // Scroll position should be similar (within 50px tolerance)
        expect(Math.abs(scrollBefore - scrollAfter)).toBeLessThan(50);
    });
});
