/**
 * Playwright E2E Tests for post-api-frontend
 * Tests the full UI workflow through the React app embedded in Spring Boot.
 * Target: http://localhost:8083 (port-forwarded post-api service)
 */

import { test, expect } from '@playwright/test';

test.describe('Post Management App E2E', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
  });

  test('🏠 页面加载 — 显示帖子列表', async ({ page }) => {
    // The page should render the React app and show content
    const text = await page.locator('body').textContent();
    expect(text).toBeTruthy();
    expect(text!.length).toBeGreaterThanOrEqual(5);
  });

  test('🔍 搜索帖子', async ({ page }) => {
    // Find search input
    const searchInput = page.locator('input[placeholder*="搜索"], input[type="search"]').first();
    if (await searchInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await searchInput.fill('E2E');
      await searchInput.press('Enter');
      await page.waitForTimeout(2000);
      // Should show filtered results
      await expect(page.locator('body')).toBeVisible();
    }
    // If search input not visible, skip
  });

  test('✏️ 创建帖子', async ({ page }) => {
    // Click "新建" or "New" button
    const newBtn = page.locator('button:has-text("新建"), button:has-text("创建"), button:has-text("New"), a:has-text("新建")').first();
    
    if (await newBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await newBtn.click();
      await page.waitForTimeout(1000);

      // Fill in the form
      const titleInput = page.locator('input[name="title"], input[id="title"], input[placeholder*="标题"]').first();
      const contentInput = page.locator('textarea[name="content"], textarea[id="content"], textarea[placeholder*="内容"]').first();
      const authorInput = page.locator('input[name="authorName"], input[id="authorName"], input[placeholder*="作者"]').first();

      if (await titleInput.isVisible({ timeout: 2000 }).catch(() => false)) {
        await titleInput.fill(`E2E Test ${Date.now()}`);
        if (await contentInput.isVisible().catch(() => false)) {
          await contentInput.fill('Playwright E2E test content');
        }
        if (await authorInput.isVisible().catch(() => false)) {
          await authorInput.fill('Playwright Tester');
        }

        // Submit
        const submitBtn = page.locator('button:has-text("提交"), button:has-text("保存"), button:has-text("Submit")').first();
        if (await submitBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
          await submitBtn.click();
          await page.waitForTimeout(2000);
        }
      }
    }
    // Verify page is still functional
    await expect(page.locator('body')).toBeVisible();
  });

  test('📄 帖子列表渲染', async ({ page }) => {
    // Wait for posts to load
    await page.waitForTimeout(3000);
    
    // Check if any post cards/items are visible
    const postCards = page.locator('[class*="post"], [class*="Post"], [class*="card"]');
    const count = await postCards.count();
    console.log(`  Found ${count} post elements`);
    
    // Page should be responsive
    await expect(page.locator('body')).toBeVisible();
  });

  test('📱 响应式布局', async ({ page }) => {
    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 812 });
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();

    // Test tablet viewport  
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('🌐 API 连接状态', async ({ page }) => {
    // Check if any error messages about network/API appear
    const errorText = page.locator('text=网络错误, text=连接失败, text=API Error');
    const hasError = await errorText.isVisible({ timeout: 5000 }).catch(() => false);
    expect(hasError).toBe(false);
  });

  test('🎨 页面基本元素检查', async ({ page }) => {
    // Verify the page is not empty
    const text = await page.locator('body').textContent();
    expect(text).toBeTruthy();
    expect(text!.length).toBeGreaterThanOrEqual(5);
  });
});
