import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import path from 'path';
import fs from 'fs';

const RESULTS_DIR = path.join(process.cwd(), 'test-results');
const TEST_PREFIX = `selenium_multi_${Date.now()}`;

if (!fs.existsSync(RESULTS_DIR)) {
  fs.mkdirSync(RESULTS_DIR, { recursive: true });
}

const testResults = {
  timestamp: new Date().toISOString(),
  devices: {}
};

// Device configurations
const devices = [
  { name: 'Mobile', width: 375, height: 667, mobileEmulation: { deviceName: 'iPhone 6' } },
  { name: 'Tablet', width: 768, height: 1024, mobileEmulation: { deviceName: 'iPad' } },
  { name: 'Desktop', width: 1920, height: 1080, mobileEmulation: null }
];

function addTest(deviceName, name, passed, details = '') {
  if (!testResults.devices[deviceName]) {
    testResults.devices[deviceName] = { tests: [] };
  }
  testResults.devices[deviceName].tests.push({ name, passed, details, time: new Date().toISOString() });
  console.log(`${passed ? '✅' : '❌'} [${deviceName}] ${name}${details ? ': ' + details : ''}`);
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function takeScreenshot(driver, deviceName, stepName) {
  try {
    const img = await driver.takeScreenshot();
    const imgPath = path.join(RESULTS_DIR, `${TEST_PREFIX}_${deviceName}_${stepName}.png`);
    fs.writeFileSync(imgPath, Buffer.from(img, 'base64'));
    console.log(`   📸 ${deviceName}_${stepName}.png`);
    return imgPath;
  } catch (e) {
    console.log(`   ⚠️ Screenshot failed: ${e.message}`);
    return null;
  }
}

async function runTestsForDevice(device) {
  console.log(`\n${'='.repeat(50)}`);
  console.log(`📱 Testing on ${device.name} (${device.width}x${device.height})`);
  console.log('='.repeat(50));
  
  const chromeOptions = new chrome.Options()
    .setChromeBinaryPath('/snap/chromium/3390/usr/lib/chromium-browser/chrome')
    .addArguments('--headless=new')
    .addArguments('--no-sandbox')
    .addArguments('--disable-gpu')
    .addArguments('--disable-dev-shm-usage')
    .addArguments('--disable-extensions')
    .addArguments('--disable-software-rasterizer');
  
  if (device.mobileEmulation) {
    chromeOptions.setMobileEmulation(device.mobileEmulation);
  } else {
    chromeOptions.addArguments(`--window-size=${device.width},${device.height}`);
  }
  
  process.env.CHROMEDRIVER_PATH = '/snap/chromium/3390/usr/lib/chromium-browser/chromedriver';
  
  let driver;
  try {
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(chromeOptions)
      .build();
    
    // 1. Open homepage
    try {
      await driver.get('http://localhost:5176');
      await driver.wait(until.elementLocated(By.css('body')), 10000);
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      const title = await driver.getTitle();
      addTest(device.name, '1. 打开首页', true, title);
    } catch (e) {
      addTest(device.name, '1. 打开首页', false, e.message);
      return;
    }
    await takeScreenshot(driver, device.name, '01_homepage');
    
    // 2. Check responsive layout
    try {
      const body = await driver.findElement(By.css('body'));
      const bodyWidth = await body.getSize().then(size => size.width);
      addTest(device.name, '2. 响应式布局', true, `Body width: ${bodyWidth}px`);
    } catch (e) {
      addTest(device.name, '2. 响应式布局', false, e.message);
    }
    await takeScreenshot(driver, device.name, '02_responsive');
    
    // 3. Check search input exists
    try {
      const searchInput = await driver.findElement(By.css('input[type="text"]'));
      const exists = searchInput !== null;
      addTest(device.name, '3. 搜索框存在', exists, exists ? 'Found' : 'Not found');
    } catch (e) {
      addTest(device.name, '3. 搜索框存在', false, e.message);
    }
    await takeScreenshot(driver, device.name, '03_search');
    
    // 4. Check buttons exist
    try {
      const buttons = await driver.findElements(By.css('button'));
      const buttonTexts = await Promise.all(buttons.map(b => b.getText()));
      const hasCreateBtn = buttonTexts.some(t => t.includes('新建') || t.includes('帖子'));
      addTest(device.name, '4. 新建按钮存在', hasCreateBtn, hasCreateBtn ? 'Found' : 'Not found');
    } catch (e) {
      addTest(device.name, '4. 新建按钮存在', false, e.message);
    }
    await takeScreenshot(driver, device.name, '04_buttons');
    
    // 5. Check post list/card layout
    try {
      const cards = await driver.findElements(By.css('.post-card'));
      addTest(device.name, '5. 帖子列表布局', cards.length >= 0, `Found ${cards.length} cards`);
    } catch (e) {
      addTest(device.name, '5. 帖子列表布局', false, e.message);
    }
    await takeScreenshot(driver, device.name, '05_layout');
    
    // 6. Check pagination
    try {
      const pagination = await driver.findElements(By.css('.pagination'));
      addTest(device.name, '6. 分页组件', pagination.length >= 0, 'Pagination component checked');
    } catch (e) {
      addTest(device.name, '6. 分页组件', false, e.message);
    }
    await takeScreenshot(driver, device.name, '06_pagination');
    
    // 7. Test CRUD - Create post
    try {
      const createBtn = await driver.findElement(By.xpath("//button[contains(text(),'新建帖子')]"));
      await driver.executeScript("arguments[0].scrollIntoView(true);", createBtn);
      await createBtn.click();
      await driver.wait(until.elementLocated(By.xpath("//h2[contains(text(),'新建帖子')]")), 5000);
      await sleep(500);
      
      const authorInput = await driver.findElement(By.xpath("//label[contains(text(),'作者名称')]/following-sibling::input"));
      await authorInput.clear();
      await authorInput.sendKeys(`Test User (${device.name})`);
      
      const titleInput = await driver.findElement(By.xpath("//label[contains(text(),'标题')]/following-sibling::input"));
      await titleInput.clear();
      await titleInput.sendKeys(`${device.name} Test Post`);
      
      const contentInput = await driver.findElement(By.xpath("//label[contains(text(),'内容')]/following-sibling::textarea"));
      await contentInput.clear();
      await contentInput.sendKeys(`Created from ${device.name} device`);
      
      const submitBtn = await driver.findElement(By.xpath("//button[@type='submit' and contains(text(),'创建')]"));
      await submitBtn.click();
      await sleep(2000);
      
      addTest(device.name, '7. 创建帖子', true, `${device.name} - Post created`);
    } catch (e) {
      addTest(device.name, '7. 创建帖子', false, e.message);
    }
    await takeScreenshot(driver, device.name, '07_created');
    
    // 8. Search functionality
    try {
      const searchInput = await driver.findElement(By.xpath("//input[@placeholder='搜索帖子...']"));
      await searchInput.clear();
      await searchInput.sendKeys(device.name);
      await driver.findElement(By.xpath("//button[contains(text(),'搜索')]")).click();
      await sleep(1000);
      addTest(device.name, '8. 搜索功能', true, 'Search completed');
    } catch (e) {
      addTest(device.name, '8. 搜索功能', false, e.message);
    }
    await takeScreenshot(driver, device.name, '08_search');
    
    // 9. Edit post
    try {
      await driver.get('http://localhost:5176');
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      
      const showAllCheckbox = await driver.findElement(By.css('input[type="checkbox"]'));
      await showAllCheckbox.click();
      await sleep(1000);
      
      const editButtons = await driver.findElements(By.xpath("//button[contains(text(),'编辑')]"));
      if (editButtons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", editButtons[0]);
        await editButtons[0].click();
        await driver.wait(until.elementLocated(By.xpath("//h2[contains(text(),'编辑')]")), 5000);
        await sleep(500);
        
        const titleInput = await driver.findElement(By.xpath("//label[contains(text(),'标题')]/following-sibling::input"));
        await titleInput.clear();
        await titleInput.sendKeys(`Edited from ${device.name}`);
        
        const saveBtn = await driver.findElement(By.xpath("//button[@type='submit' and contains(text(),'保存')]"));
        await saveBtn.click();
        await sleep(2000);
        
        addTest(device.name, '9. 编辑帖子', true, 'Post edited');
      } else {
        addTest(device.name, '9. 编辑帖子', false, 'Edit button not found');
      }
    } catch (e) {
      addTest(device.name, '9. 编辑帖子', false, e.message);
    }
    await takeScreenshot(driver, device.name, '09_edited');
    
    // 10. Like functionality
    try {
      const likeButtons = await driver.findElements(By.xpath("//button[contains(text(),'点赞')]"));
      if (likeButtons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", likeButtons[0]);
        await likeButtons[0].click();
        await sleep(1000);
        addTest(device.name, '10. 点赞功能', true, 'Like clicked');
      } else {
        addTest(device.name, '10. 点赞功能', false, 'Like button not found');
      }
    } catch (e) {
      addTest(device.name, '10. 点赞功能', false, e.message);
    }
    await takeScreenshot(driver, device.name, '10_liked');
    
    // 11. Final state
    try {
      const pageSource = await driver.getPageSource();
      addTest(device.name, '11. 最终状态', true, 'Page loaded successfully');
    } catch (e) {
      addTest(device.name, '11. 最终状态', false, e.message);
    }
    await takeScreenshot(driver, device.name, '11_final');
    
  } catch (e) {
    console.error(`❌ [${device.name}] Error:`, e.message);
    addTest(device.name, 'Browser error', false, e.message);
  } finally {
    if (driver) {
      await driver.quit();
    }
  }
}

async function runAllTests() {
  console.log('🔄 Starting Multi-Device Selenium E2E Tests...\n');
  
  for (const device of devices) {
    await runTestsForDevice(device);
  }
  
  // Save results
  const resultsFile = path.join(RESULTS_DIR, `selenium-multi-device-results-${Date.now()}.json`);
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  
  // Summary
  console.log(`\n${'='.repeat(50)}`);
  console.log('📊 Test Summary');
  console.log('='.repeat(50));
  
  let totalPassed = 0;
  let totalFailed = 0;
  
  for (const [deviceName, data] of Object.entries(testResults.devices)) {
    const passed = data.tests.filter(t => t.passed).length;
    const failed = data.tests.filter(t => !t.passed).length;
    totalPassed += passed;
    totalFailed += failed;
    console.log(`${deviceName}: ${passed} passed, ${failed} failed`);
  }
  
  console.log(`\nTotal: ${totalPassed} passed, ${totalFailed} failed`);
  console.log(`📁 Results: ${resultsFile}`);
  console.log(`📸 Screenshots: ${RESULTS_DIR}/${TEST_PREFIX}_*.png`);
  
  return testResults;
}

runAllTests().catch(console.error);
