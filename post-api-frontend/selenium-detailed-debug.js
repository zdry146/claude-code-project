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
  tests: [],
  consoleLogs: []
};

function addTest(name, passed, details = '') {
  testResults.tests.push({ name, passed, details, time: new Date().toISOString() });
  console.log(`${passed ? '✅' : '❌'} ${name}${details ? ': ' + details : ''}`);
}

async function runDetailedDebugTests() {
  console.log('🔄 Starting Detailed Debug Selenium Tests...\n');

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

    // Capture console logs
    await driver.executeScript(`
      window.__consoleLogs = [];
      window.__originalConsoleLog = console.log;
      window.__originalConsoleError = console.error;
      window.__originalConsoleWarn = console.warn;
      console.log = (...args) => {
        window.__consoleLogs.push({ type: 'log', message: args.join(' ') });
        window.__originalConsoleLog.apply(console, args);
      };
      console.error = (...args) => {
        window.__consoleLogs.push({ type: 'error', message: args.join(' ') });
        window.__originalConsoleError.apply(console, args);
      };
      console.warn = (...args) => {
        window.__consoleLogs.push({ type: 'warn', message: args.join(' ') });
        window.__originalConsoleWarn.apply(console, args);
      };
    `);

    // Test 1: Open frontend
    try {
      await driver.get('http://localhost:5173');
      await driver.wait(until.elementLocated(By.css('body')), 5000);
      const title = await driver.getTitle();
      addTest('Open frontend page', true, `Title: ${title}`);
    } catch (e) {
      addTest('Open frontend page', false, e.message);
    }

    // Wait for potential rendering
    await driver.sleep(3000);

    // Test 2: Debug - take screenshot
    try {
      const screenshot = await driver.takeScreenshot();
      const screenshotPath = path.join(RESULTS_DIR, 'detailed_debug_homepage.png');
      fs.writeFileSync(screenshotPath, Buffer.from(screenshot, 'base64'));
      addTest('Take debug screenshot', true, `Saved: detailed_debug_homepage.png`);
    } catch (e) {
      addTest('Take debug screenshot', false, e.message);
    }

    // Test 3: Get ALL HTML in body
    try {
      const bodyHTML = await driver.executeScript(`
        return document.body.innerHTML;
      `);
      addTest('Get body HTML', true, `Length: ${bodyHTML.length}, Preview: ${bodyHTML.substring(0, 200)}`);
    } catch (e) {
      addTest('Get body HTML', false, e.message);
    }

    // Test 4: Get all elements
    try {
      const elements = await driver.executeScript(`
        const all = document.querySelectorAll('*');
        const tags = {};
        for (let el of all) {
          tags[el.tagName] = (tags[el.tagName] || 0) + 1;
        }
        return JSON.stringify(tags);
      `);
      addTest('Count all HTML elements', true, `Elements: ${elements}`);
    } catch (e) {
      addTest('Count all HTML elements', false, e.message);
    }

    // Test 5: Check #root content
    try {
      const rootContent = await driver.executeScript(`
        const root = document.getElementById('root');
        return root ? root.innerHTML.substring(0, 500) : 'NO ROOT FOUND';
      `);
      addTest('Check #root content', true, `Content: ${rootContent}`);
    } catch (e) {
      addTest('Check #root content', false, e.message);
    }

    // Test 6: Get console logs
    try {
      const logs = await driver.executeScript(`return window.__consoleLogs || []`);
      testResults.consoleLogs = logs;
      addTest('Get console logs', true, `Found ${logs.length} logs`);
      logs.slice(0, 5).forEach(log => {
        console.log(`  [${log.type}] ${log.message.substring(0, 100)}`);
      });
    } catch (e) {
      addTest('Get console logs', false, e.message);
    }

    // Test 7: Try to find ANY visible text
    try {
      const bodyText = await driver.executeScript(`
        return document.body.innerText.substring(0, 300);
      `);
      addTest('Get body text content', true, `Text: "${bodyText}"`);
    } catch (e) {
      addTest('Get body text content', false, e.message);
    }

    // Test 8: Check for specific elements
    const selectors = ['.app-container', 'h1', '.empty', '.loading', 'header', '[class]'];
    for (const selector of selectors) {
      try {
        const count = await driver.executeScript(`
          return document.querySelectorAll('${selector}').length;
        `);
        console.log(`  ${selector}: ${count} elements`);
      } catch (e) {
        console.log(`  ${selector}: error - ${e.message}`);
      }
    }

    // Test 9: Check network requests for errors
    try {
      const apiResult = await driver.executeScript(`
        return fetch('http://localhost:8080/api/posts/published?page=0&size=10')
          .then(r => r.text())
          .then(d => ({ success: true, data: d.substring(0, 200) }))
          .catch(e => ({ success: false, error: e.message }));
      `);
      addTest('Direct API call from browser', apiResult.success, apiResult.data || apiResult.error);
    } catch (e) {
      addTest('Direct API call from browser', false, e.message);
    }

  } catch (e) {
    console.error('❌ Failed to start browser:', e.message);
    addTest('Browser initialization', false, e.message);
  } finally {
    if (driver) {
      await driver.quit();
    }
  }

  const resultsFile = path.join(RESULTS_DIR, 'detailed-debug-results.json');
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  console.log(`\n📁 Results saved to: ${resultsFile}`);

  const passed = testResults.tests.filter(t => t.passed).length;
  const failed = testResults.tests.filter(t => !t.passed).length;
  console.log(`\n📊 Test Summary: ${passed} passed, ${failed} failed`);

  return testResults;
}

runDetailedDebugTests().catch(console.error);