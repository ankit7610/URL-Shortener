import { test, expect } from '@playwright/test';

test.describe('Home Page - Basic Functionality', () => {
    test('should load the home page successfully', async ({ page }) => {
        await page.goto('/');

        // Check page title
        await expect(page).toHaveTitle(/URL Shortener/);

        // Check main heading is visible
        const heading = page.getByRole('heading', { name: /Shorten URLs with/i });
        await expect(heading).toBeVisible();

        // Check URL input field exists
        const urlInput = page.getByPlaceholder(/Paste your long URL/i);
        await expect(urlInput).toBeVisible();

        // Check shorten button exists
        const shortenButton = page.getByRole('button', { name: /Shorten URL/i });
        await expect(shortenButton).toBeVisible();
    });

    test('should display all feature cards', async ({ page }) => {
        await page.goto('/');

        // Check all 4 feature cards are visible
        await expect(page.getByText('Advanced Analytics')).toBeVisible();
        await expect(page.getByText('Custom Short Links')).toBeVisible();
        await expect(page.getByText('Password Protection')).toBeVisible();
        await expect(page.getByText('QR Codes')).toBeVisible();
    });

    test('should display navigation links', async ({ page }) => {
        await page.goto('/');

        // Check header navigation
        await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
        await expect(page.getByRole('link', { name: 'Login' })).toBeVisible();
        await expect(page.getByRole('button', { name: /Sign Up/i })).toBeVisible();
    });

    test('should display statistics section', async ({ page }) => {
        await page.goto('/');

        // Check stats are visible
        await expect(page.getByText('10M+')).toBeVisible();
        await expect(page.getByText('Links Created')).toBeVisible();
        await expect(page.getByText('100M+')).toBeVisible();
        await expect(page.getByText('Clicks Tracked')).toBeVisible();
        await expect(page.getByText('50K+')).toBeVisible();
        await expect(page.getByText('Active Users')).toBeVisible();
    });
});
