import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import path from 'path';
import fs from 'fs';

const RESULTS_DIR = path.join(process.cwd(), 'test-results');

if (!fs.existsSync(RESULTS_DIR)) {
  fs.mkdirSync(RESULTS_DIR, { recursive: true });
}

const testResults = {
  timestamp: new Date().toISOString(),
  tests: []
};

function addTest(name, passed, details = '') {
  testResults.tests.push({ name, passed, details, time: new Date().toISOString() });
  console.log(`${passed ? '✅' : '❌'} ${name}${details ? ': ' + details : ''}`);
}

async function runDebugTests() {
  console.log('🔄 Starting Debug Selenium Tests...\n');

  const chromeOptions = new chrome.Options()
    .setChromeBinaryPath('/snap/chromium/3390/usr/lib/chromium-browser/chrome')
    .addArguments('--headless')
    .addArguments('--no-sandbox')
    .addArguments('--disable-gpu')
    .addArguments('--disable-dev-shm-usage')
    .addArguments('--window-size=1280,720')
    .addArguments('--disable-extensions')
    .addArguments('--disable-software-rasterizer');

  process.env.CHROMEDRIVER_PATH = '/snap/chromium/3390/usr/lib/chromium-browser/chromedriver';

  let driver;
  try {
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(chromeOptions)
      .build();

    console.log('✅ Browser started successfully\n');

    // Test 1: Open frontend
    try {
      await driver.get('http://localhost:5173');
      await driver.wait(until.elementLocated(By.css('body')), 5000);
      const title = await driver.getTitle();
      addTest('Open frontend page', true, `Title: ${title}`);
    } catch (e) {
      addTest('Open frontend page', false, e.message);
    }

    // Test 2: Debug - take screenshot immediately
    try {
      await driver.sleep(2000); // Wait for React to render
      const screenshot = await driver.takeScreenshot();
      const screenshotPath = path.join(RESULTS_DIR, 'debug_homepage.png');
      fs.writeFileSync(screenshotPath, Buffer.from(screenshot, 'base64'));
      addTest('Take debug screenshot', true, `Saved: debug_homepage.png`);
    } catch (e) {
      addTest('Take debug screenshot', false, e.message);
    }

    // Test 3: Check page source
    try {
      const pageSource = await driver.getPageSource();
      const hasReactRoot = pageSource.includes('root');
      const hasAppTitle = pageSource.includes('帖子');
      addTest('Check page source', hasReactRoot && hasAppTitle,
        `React root: ${hasReactRoot}, Chinese text: ${hasAppTitle}`);
    } catch (e) {
      addTest('Check page source', false, e.message);
    }

    // Test 4: Try multiple selectors for header
    const selectors = ['h1', 'h1.app-title', '.app-title', '[class*="title"]'];
    for (const selector of selectors) {
      try {
        const elements = await driver.findElements(By.css(selector));
        if (elements.length > 0) {
          const text = await elements[0].getText();
          addTest(`Find header with selector: ${selector}`, true, `Found: "${text}"`);
          break;
        }
      } catch (e) {
        // Continue to next selector
      }
    }

    // Test 5: Check for error banner
    try {
      const errorElements = await driver.findElements(By.css('.error-banner'));
      if (errorElements.length > 0) {
        const errorText = await errorElements[0].getText();
        addTest('Check for error banner', true, `Error shown: "${errorText}"`);
      } else {
        addTest('Check for error banner', true, 'No error banner');
      }
    } catch (e) {
      addTest('Check for error banner', false, e.message);
    }

    // Test 6: Check for loading state
    try {
      const loadingElements = await driver.findElements(By.css('.loading'));
      if (loadingElements.length > 0) {
        addTest('Check loading state', true, 'Page is loading');
      } else {
        addTest('Check loading state', true, 'Not in loading state');
      }
    } catch (e) {
      addTest('Check loading state', false, e.message);
    }

    // Test 7: Check for app container
    try {
      await driver.wait(until.elementLocated(By.css('.app-container')), 5000);
      addTest('Check app container exists', true, 'Found .app-container');
    } catch (e) {
      addTest('Check app container exists', false, e.message);
    }

    // Test 8: Find search input
    try {
      await driver.wait(until.elementLocated(By.css('input[type="text"]')), 5000);
      addTest('Find search input', true, 'Found text input');
    } catch (e) {
      addTest('Find search input', false, e.message);
    }

    // Test 9: Find create button
    try {
      const buttons = await driver.findElements(By.css('button'));
      let found = false;
      for (const btn of buttons) {
        const text = await btn.getText();
        if (text.includes('新建') || text.includes('帖子')) {
          addTest('Find create button', true, `Button: "${text}"`);
          found = true;
          break;
        }
      }
      if (!found) {
        addTest('Find create button', false, 'Button not found');
      }
    } catch (e) {
      addTest('Find create button', false, e.message);
    }

    // Test 10: API connectivity test
    try {
      const apiResponse = await driver.executeScript(`
        return fetch('http://localhost:8080/api/posts/published?page=0&size=10')
          .then(r => r.json())
          .then(d => ({ success: true, data: d }))
          .catch(e => ({ success: false, error: e.message }));
      `);
      if (apiResponse.success) {
        addTest('API connectivity', true, `Backend responding: ${JSON.stringify(apiResponse.data).substring(0, 50)}...`);
      } else {
        addTest('API connectivity', false, apiResponse.error);
      }
    } catch (e) {
      addTest('API connectivity', false, e.message);
    }

  } catch (e) {
    console.error('❌ Failed to start browser:', e.message);
    addTest('Browser initialization', false, e.message);
  } finally {
    if (driver) {
      await driver.quit();
    }
  }

  const resultsFile = path.join(RESULTS_DIR, 'debug-test-results.json');
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  console.log(`\n📁 Results saved to: ${resultsFile}`);

  const passed = testResults.tests.filter(t => t.passed).length;
  const failed = testResults.tests.filter(t => !t.passed).length;
  console.log(`\n📊 Test Summary: ${passed} passed, ${failed} failed`);

  return testResults;
}

runDebugTests().catch(console.error);