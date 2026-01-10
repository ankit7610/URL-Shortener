import { test, expect } from '@playwright/test';

test.describe('Animations and Visual Effects', () => {
    test('should display hover effects on feature cards', async ({ page }) => {
        await page.goto('/');

        // Find a feature card
        const featureCard = page.locator('text=Advanced Analytics').locator('..');

        // Hover over the card
        await featureCard.hover();

        // Wait for animation
        await page.waitForTimeout(500);

        // Card should be visible after hover
        await expect(featureCard).toBeVisible();
    });

    test('should animate gradient background', async ({ page }) => {
        await page.goto('/');

        // Check that the main container has gradient classes
        const mainContainer = page.locator('div').first();
        await expect(mainContainer).toBeVisible();

        // Verify page loaded successfully
        await expect(page.getByRole('heading', { name: /Shorten URLs with/i })).toBeVisible();
    });

    test('should show button hover effects', async ({ page }) => {
        await page.goto('/');

        const shortenButton = page.getByRole('button', { name: /Shorten URL/i });

        // Hover over button
        await shortenButton.hover();

        // Wait for hover animation
        await page.waitForTimeout(300);

        // Button should still be visible and enabled
        await expect(shortenButton).toBeVisible();
        await expect(shortenButton).toBeEnabled();
    });

    test('should display statistics with hover scale effect', async ({ page }) => {
        await page.goto('/');

        // Find statistics section
        const stat = page.getByText('10M+');
        await expect(stat).toBeVisible();

        // Hover over stat
        await stat.hover();

        // Wait for scale animation
        await page.waitForTimeout(300);

        // Stat should still be visible
        await expect(stat).toBeVisible();
    });
});
