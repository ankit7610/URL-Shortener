import { test, expect } from '@playwright/test';

test.describe('Theme Toggle Functionality', () => {
    test('should toggle between light and dark themes', async ({ page }) => {
        await page.goto('/');

        // Find theme toggle button
        const themeToggle = page.locator('button[aria-label="Toggle theme"]');
        await expect(themeToggle).toBeVisible();

        // Get initial theme (check html class)
        const html = page.locator('html');
        const initialClass = await html.getAttribute('class');

        // Click theme toggle
        await themeToggle.click();

        // Wait for theme change
        await page.waitForTimeout(500);

        // Verify theme changed
        const newClass = await html.getAttribute('class');
        expect(newClass).not.toBe(initialClass);
    });

    test('should persist theme preference', async ({ page, context }) => {
        await page.goto('/');

        // Toggle to dark mode
        const themeToggle = page.locator('button[aria-label="Toggle theme"]');
        await themeToggle.click();
        await page.waitForTimeout(500);

        // Get current theme
        const html = page.locator('html');
        const darkModeClass = await html.getAttribute('class');

        // Reload page
        await page.reload();
        await page.waitForTimeout(500);

        // Check theme persisted
        const reloadedClass = await html.getAttribute('class');
        expect(reloadedClass).toBe(darkModeClass);
    });

    test('should display correct theme icon', async ({ page }) => {
        await page.goto('/');

        const themeToggle = page.locator('button[aria-label="Toggle theme"]');

        // Check that either sun or moon icon is visible
        const hasSunOrMoon = await themeToggle.locator('svg').count();
        expect(hasSunOrMoon).toBeGreaterThan(0);
    });

    test('should show theme tooltip on hover', async ({ page }) => {
        await page.goto('/');

        const themeToggle = page.locator('button[aria-label="Toggle theme"]');

        // Hover over theme toggle
        await themeToggle.hover();

        // Wait for tooltip to appear
        await page.waitForTimeout(300);

        // Check for tooltip text (either "Light mode" or "Dark mode")
        const tooltipVisible = await page.locator('text=/Light mode|Dark mode/').isVisible();
        expect(tooltipVisible).toBeTruthy();
    });
});
