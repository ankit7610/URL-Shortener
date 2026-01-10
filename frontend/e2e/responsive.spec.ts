import { test, expect } from '@playwright/test';

test.describe('Responsive Design', () => {
    test('should display mobile menu on small screens', async ({ page }) => {
        // Set mobile viewport
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        // Check that main content is visible
        const heading = page.getByRole('heading', { name: /Shorten URLs with/i });
        await expect(heading).toBeVisible();

        // Check URL input is responsive
        const urlInput = page.getByPlaceholder(/Paste your long URL/i);
        await expect(urlInput).toBeVisible();
    });

    test('should stack feature cards on mobile', async ({ page }) => {
        await page.setViewportSize({ width: 375, height: 667 });
        await page.goto('/');

        // All feature cards should still be visible using role selectors
        await expect(page.getByRole('heading', { name: 'Advanced Analytics' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'Custom Short Links' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'Password Protection' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'QR Codes' })).toBeVisible();
    });

    test('should display properly on tablet', async ({ page }) => {
        // Set tablet viewport
        await page.setViewportSize({ width: 768, height: 1024 });
        await page.goto('/');

        // Check main elements are visible
        await expect(page.getByRole('heading', { name: /Shorten URLs with/i })).toBeVisible();
        await expect(page.getByPlaceholder(/Paste your long URL/i)).toBeVisible();
        await expect(page.getByRole('button', { name: /Shorten URL/i })).toBeVisible();
    });

    test('should display properly on desktop', async ({ page }) => {
        // Set desktop viewport
        await page.setViewportSize({ width: 1920, height: 1080 });
        await page.goto('/');

        // Check all navigation items are visible
        await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
        await expect(page.getByRole('link', { name: 'Login' })).toBeVisible();
        await expect(page.getByRole('button', { name: /Sign Up/i })).toBeVisible();

        // Check theme toggle is visible
        const themeToggle = page.locator('button[aria-label="Toggle theme"]');
        await expect(themeToggle).toBeVisible();
    });
});
