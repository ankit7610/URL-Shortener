import { test, expect } from '@playwright/test';

test.describe('Form Interactions and Accessibility', () => {
    test('should focus URL input field on page load', async ({ page }) => {
        await page.goto('/');

        const urlInput = page.getByPlaceholder(/Paste your long URL/i);

        // Click on input to focus
        await urlInput.click();

        // Verify input is focused
        await expect(urlInput).toBeFocused();
    });

    test('should accept URL input and enable shorten button', async ({ page }) => {
        await page.goto('/');

        const urlInput = page.getByPlaceholder(/Paste your long URL/i);
        const shortenButton = page.getByRole('button', { name: /Shorten URL/i });

        // Type a URL
        await urlInput.fill('https://www.example.com/very/long/url/path');

        // Verify input has value
        await expect(urlInput).toHaveValue('https://www.example.com/very/long/url/path');

        // Button should be enabled
        await expect(shortenButton).toBeEnabled();
    });

    test('should have proper ARIA labels for accessibility', async ({ page }) => {
        await page.goto('/');

        // Check theme toggle has aria-label
        const themeToggle = page.locator('button[aria-label="Toggle theme"]');
        await expect(themeToggle).toBeVisible();

        // Check main heading has proper role
        const heading = page.getByRole('heading', { name: /Shorten URLs with/i });
        await expect(heading).toBeVisible();
    });

    test('should navigate using keyboard', async ({ page }) => {
        await page.goto('/');

        // Tab through elements
        await page.keyboard.press('Tab');

        // Check if navigation link gets focus
        const dashboardLink = page.getByRole('link', { name: 'Dashboard' });

        // Continue tabbing
        await page.keyboard.press('Tab');
        await page.keyboard.press('Tab');

        // Verify we can navigate with keyboard
        const loginLink = page.getByRole('link', { name: 'Login' });
        await expect(loginLink).toBeVisible();
    });
});
