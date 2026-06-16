import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import path from 'path';
import fs from 'fs';

const RESULTS_DIR = path.join(process.cwd(), 'test-results');

// Ensure results directory exists
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

async function runSeleniumTests() {
  console.log('🔄 Starting Selenium E2E Tests...\n');
  
  // Configure Chrome options
  const chromeOptions = new chrome.Options()
    .setChromeBinaryPath('/usr/bin/chromium-browser')
    .addArguments('--headless')
    .addArguments('--no-sandbox')
    .addArguments('--disable-gpu')
    .addArguments('--disable-dev-shm-usage')
    .addArguments('--window-size=1280,720')
    .addArguments('--disable-extensions')
    .addArguments('--disable-software-rasterizer');
  
  // Configure chromedriver path
  process.env.CHROMEDRIVER_PATH = '/snap/chromium/current/usr/lib/chromium-browser/chromedriver';
  
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
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      const title = await driver.getTitle();
      addTest('Open frontend page', true, `Title: ${title}`);
    } catch (e) {
      addTest('Open frontend page', false, e.message);
    }
    
    // Test 2: Check header text
    try {
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      const headerText = await driver.findElement(By.css('h1')).getText();
      const hasChinese = headerText.includes('帖子');
      addTest('Check page header', hasChinese, `Found: "${headerText}"`);
    } catch (e) {
      addTest('Check page header', false, e.message);
    }
    
    // Test 3: Take screenshot
    try {
      const screenshot = await driver.takeScreenshot();
      const screenshotPath = path.join(RESULTS_DIR, 'selenium_homepage.png');
      fs.writeFileSync(screenshotPath, Buffer.from(screenshot, 'base64'));
      addTest('Take homepage screenshot', true, `Saved: selenium_homepage.png`);
    } catch (e) {
      addTest('Take homepage screenshot', false, e.message);
    }
    
    // Test 4: Check search input exists
    try {
      const searchInput = await driver.findElement(By.css('input[type="text"]'));
      const exists = searchInput !== null;
      addTest('Check search input exists', exists, exists ? 'Found' : 'Not found');
    } catch (e) {
      addTest('Check search input exists', false, e.message);
    }
    
    // Test 5: Check create button exists
    try {
      const buttons = await driver.findElements(By.css('button'));
      let foundCreateBtn = false;
      for (const btn of buttons) {
        const btnText = await btn.getText();
        if (btnText.includes('新建') || btnText.includes('帖子')) {
          foundCreateBtn = true;
          addTest('Check create button exists', true, `Button: "${btnText}"`);
          break;
        }
      }
      if (!foundCreateBtn) {
        addTest('Check create button exists', false, 'Create button not found');
      }
    } catch (e) {
      addTest('Check create button exists', false, e.message);
    }
    
    // Test 6: Navigate to Swagger UI (backend API docs)
    try {
      await driver.get('http://localhost:8080/swagger-ui.html');
      await driver.wait(until.elementLocated(By.css('body')), 5000);
      const pageSource = await driver.getPageSource();
      const hasSwagger = pageSource.includes('swagger') || pageSource.includes('Swagger');
      addTest('Open Swagger UI', hasSwagger, hasSwagger ? 'Swagger UI loaded' : 'Swagger UI not found');
    } catch (e) {
      addTest('Open Swagger UI', false, e.message);
    }
    
    // Test 7: Take Swagger screenshot
    try {
      const screenshot = await driver.takeScreenshot();
      const screenshotPath = path.join(RESULTS_DIR, 'selenium_swagger.png');
      fs.writeFileSync(screenshotPath, Buffer.from(screenshot, 'base64'));
      addTest('Take Swagger screenshot', true, `Saved: selenium_swagger.png`);
    } catch (e) {
      addTest('Take Swagger screenshot', false, e.message);
    }
    
  } catch (e) {
    console.error('❌ Failed to start browser:', e.message);
    addTest('Browser initialization', false, e.message);
  } finally {
    if (driver) {
      await driver.quit();
    }
  }
  
  // Save results
  const resultsFile = path.join(RESULTS_DIR, 'selenium-test-results.json');
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  console.log(`\n📁 Results saved to: ${resultsFile}`);
  
  // Summary
  const passed = testResults.tests.filter(t => t.passed).length;
  const failed = testResults.tests.filter(t => !t.passed).length;
  console.log(`\n📊 Test Summary: ${passed} passed, ${failed} failed`);
  
  return testResults;
}

runSeleniumTests().catch(console.error);
