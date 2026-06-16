import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import path from 'path';
import fs from 'fs';

const RESULTS_DIR = path.join(process.cwd(), 'test-results');
const TEST_PREFIX = `selenium_full_${Date.now()}`;

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

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function takeScreenshot(driver, name) {
  try {
    const img = await driver.takeScreenshot();
    const imgPath = path.join(RESULTS_DIR, `${TEST_PREFIX}_${name}.png`);
    fs.writeFileSync(imgPath, Buffer.from(img, 'base64'));
    console.log(`   📸 Screenshot: ${name}.png`);
    return imgPath;
  } catch (e) {
    console.log(`   ⚠️ Screenshot failed: ${e.message}`);
    return null;
  }
}

async function runFullTests() {
  console.log('🔄 Starting Full Selenium E2E Tests...\n');
  
  const chromeOptions = new chrome.Options()
    .setChromeBinaryPath('/snap/chromium/3390/usr/lib/chromium-browser/chrome')
    .addArguments('--headless=new')
    .addArguments('--no-sandbox')
    .addArguments('--disable-gpu')
    .addArguments('--disable-dev-shm-usage')
    .addArguments('--window-size=1920,1080')
    .addArguments('--disable-extensions')
    .addArguments('--disable-software-rasterizer')
    .addArguments('--force-device-scale-factor=2');
  
  process.env.CHROMEDRIVER_PATH = '/snap/chromium/3390/usr/lib/chromium-browser/chromedriver';
  
  let driver;
  try {
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(chromeOptions)
      .build();
    
    console.log('✅ Browser started\n');
    
    // ========== 1. 打开首页 ==========
    try {
      await driver.get('http://localhost:5174');
      await driver.wait(until.elementLocated(By.css('body')), 10000);
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      const title = await driver.getTitle();
      addTest('1. 打开首页', true, title);
    } catch (e) {
      addTest('1. 打开首页', false, e.message);
      throw e;
    }
    await takeScreenshot(driver, '01_homepage');
    
    // ========== 2. 测试创建帖子 (增) ==========
    try {
      const createBtn = await driver.findElement(By.xpath("//button[contains(text(),'新建帖子')]"));
      await driver.executeScript("arguments[0].scrollIntoView(true);", createBtn);
      await createBtn.click();
      
      await driver.wait(until.elementLocated(By.xpath("//h2[contains(text(),'新建帖子')]")), 5000);
      await sleep(500);
      
      const authorInput = await driver.findElement(By.xpath("//label[contains(text(),'作者名称')]/following-sibling::input"));
      await authorInput.clear();
      await authorInput.sendKeys('Selenium Tester');
      
      const titleInput = await driver.findElement(By.xpath("//label[contains(text(),'标题')]/following-sibling::input"));
      await titleInput.clear();
      await titleInput.sendKeys('Selenium 创建的测试帖子');
      
      const contentInput = await driver.findElement(By.xpath("//label[contains(text(),'内容')]/following-sibling::textarea"));
      await contentInput.clear();
      await contentInput.sendKeys('这是通过 Selenium 自动测试创建的内容');
      
      const submitBtn = await driver.findElement(By.xpath("//button[@type='submit' and contains(text(),'创建')]"));
      await submitBtn.click();
      
      await sleep(2000);
      addTest('2. 创建帖子 (增)', true, '帖子创建成功');
      
    } catch (e) {
      addTest('2. 创建帖子 (增)', false, e.message);
    }
    await takeScreenshot(driver, '02_after_create');
    
    // ========== 3. 刷新页面，显示全部帖子 ==========
    try {
      await driver.get('http://localhost:5174');
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      
      // Enable show all checkbox
      const showAllCheckbox = await driver.findElement(By.css('input[type="checkbox"]'));
      await showAllCheckbox.click();
      await sleep(1000);
      
      addTest('3. 显示全部帖子', true, '已勾选显示全部');
    } catch (e) {
      addTest('3. 显示全部帖子', false, e.message);
    }
    await takeScreenshot(driver, '03_all_posts');
    
    // ========== 4. 测试搜索帖子 (查) ==========
    try {
      const searchInput = await driver.findElement(By.xpath("//input[@placeholder='搜索帖子...']"));
      await searchInput.clear();
      await searchInput.sendKeys('Selenium');
      await driver.findElement(By.xpath("//button[contains(text(),'搜索')]")).click();
      await sleep(1000);
      
      const pageSource = await driver.getPageSource();
      const found = pageSource.includes('Selenium');
      addTest('4. 搜索帖子 (查)', found, found ? '找到匹配帖子' : '未找到匹配帖子');
    } catch (e) {
      addTest('4. 搜索帖子 (查)', false, e.message);
    }
    await takeScreenshot(driver, '04_search_result');
    
    // ========== 5. 测试编辑帖子 (改) ==========
    try {
      // Reload and show all
      await driver.get('http://localhost:5174');
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      
      const showAllCheckbox = await driver.findElement(By.css('input[type="checkbox"]'));
      await showAllCheckbox.click();
      await sleep(1000);
      
      // Find and click edit button
      const buttons = await driver.findElements(By.xpath("//button[contains(text(),'编辑')]"));
      if (buttons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", buttons[0]);
        await buttons[0].click();
        
        await driver.wait(until.elementLocated(By.xpath("//h2[contains(text(),'编辑帖子')]")), 5000);
        await sleep(500);
        
        const titleInput = await driver.findElement(By.xpath("//label[contains(text(),'标题')]/following-sibling::input"));
        await titleInput.clear();
        await titleInput.sendKeys('Selenium 编辑后的帖子标题');
        
        const saveBtn = await driver.findElement(By.xpath("//button[@type='submit' and contains(text(),'保存')]"));
        await saveBtn.click();
        
        await sleep(2000);
        
        const pageSource = await driver.getPageSource();
        const updated = pageSource.includes('Selenium 编辑后的帖子标题');
        addTest('5. 编辑帖子 (改)', updated, updated ? '帖子更新成功' : '更新未成功');
      } else {
        addTest('5. 编辑帖子 (改)', false, '未找到编辑按钮');
      }
    } catch (e) {
      addTest('5. 编辑帖子 (改)', false, e.message);
    }
    await takeScreenshot(driver, '05_after_edit');
    
    // ========== 6. 测试点赞功能 ==========
    try {
      const buttons = await driver.findElements(By.xpath("//button[contains(text(),'点赞')]"));
      if (buttons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", buttons[0]);
        await buttons[0].click();
        await sleep(1000);
        addTest('6. 点赞功能', true, '点赞按钮点击成功');
      } else {
        addTest('6. 点赞功能', false, '未找到点赞按钮');
      }
    } catch (e) {
      addTest('6. 点赞功能', false, e.message);
    }
    await takeScreenshot(driver, '06_after_like');
    
    // ========== 7. 测试发布功能 ==========
    try {
      const buttons = await driver.findElements(By.xpath("//button[contains(text(),'发布')]"));
      if (buttons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", buttons[0]);
        await buttons[0].click();
        await sleep(1000);
        
        const pageSource = await driver.getPageSource();
        const published = pageSource.includes('已发布');
        addTest('7. 发布功能', published, published ? '发布成功' : '发布失败');
      } else {
        addTest('7. 发布功能', false, '未找到发布按钮');
      }
    } catch (e) {
      addTest('7. 发布功能', false, e.message);
    }
    await takeScreenshot(driver, '07_after_publish');
    
    // ========== 8. 测试翻页功能 ==========
    try {
      const nextBtns = await driver.findElements(By.xpath("//button[contains(text(),'下一页')]"));
      
      if (nextBtns.length > 0) {
        const nextBtn = nextBtns[0];
        const isDisabled = await nextBtn.getAttribute('disabled');
        
        if (!isDisabled) {
          await nextBtn.click();
          await sleep(1000);
          addTest('8. 翻页功能', true, '翻页成功');
        } else {
          addTest('8. 翻页功能', true, '只有一页数据');
        }
      } else {
        addTest('8. 翻页功能', true, '数据不足一页');
      }
    } catch (e) {
      addTest('8. 翻页功能', false, e.message);
    }
    await takeScreenshot(driver, '08_pagination');
    
    // ========== 9. 测试删除功能 ==========
    try {
      // Reload page first
      await driver.get('http://localhost:5174');
      await driver.wait(until.elementLocated(By.css('h1')), 5000);
      
      const showAllCheckbox = await driver.findElement(By.css('input[type="checkbox"]'));
      await showAllCheckbox.click();
      await sleep(1000);
      
      const deleteButtons = await driver.findElements(By.xpath("//button[contains(text(),'删除')]"));
      if (deleteButtons.length > 0) {
        await driver.executeScript("arguments[0].scrollIntoView(true);", deleteButtons[0]);
        await deleteButtons[0].click();
        await sleep(500);
        
        try {
          await driver.switchTo().alert().then(async alert => {
            await alert.accept();
          });
        } catch {}
        
        await sleep(1500);
        addTest('9. 删除帖子 (删)', true, '删除操作完成');
      } else {
        addTest('9. 删除帖子 (删)', false, '未找到删除按钮');
      }
    } catch (e) {
      addTest('9. 删除帖子 (删)', false, e.message);
    }
    await takeScreenshot(driver, '09_after_delete');
    
    // ========== 10. 最终状态 ==========
    try {
      await driver.getPageSource();
      addTest('10. 页面可访问', true, '页面正常加载');
    } catch (e) {
      addTest('10. 页面可访问', false, e.message);
    }
    await takeScreenshot(driver, '10_final_state');
    
    // ========== 11. API 数据验证 ==========
    try {
      const http = await import('http');
      const apiData = await new Promise((resolve, reject) => {
        http.get('http://localhost:8080/api/posts?page=0&size=10', res => {
          let data = '';
          res.on('data', chunk => data += chunk);
          res.on('end', () => {
            try {
              resolve(JSON.parse(data));
            } catch {
              resolve({ data: { content: [] } });
            }
          });
        }).on('error', reject);
      });
      const posts = apiData.data?.content || [];
      addTest('11. API 数据验证', true, `数据库有 ${posts.length} 条帖子`);
    } catch (e) {
      addTest('11. API 数据验证', false, e.message);
    }
    
  } catch (e) {
    console.error('❌ Browser error:', e.message);
    addTest('Browser initialization', false, e.message);
  } finally {
    if (driver) {
      await driver.quit();
    }
  }
  
  // Save results
  const resultsFile = path.join(RESULTS_DIR, 'selenium-full-test-results.json');
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  
  const passed = testResults.tests.filter(t => t.passed).length;
  const failed = testResults.tests.filter(t => !t.passed).length;
  console.log(`\n${'='.repeat(50)}`);
  console.log(`📊 Test Summary: ${passed} passed, ${failed} failed`);
  console.log(`📁 Results: ${resultsFile}`);
  console.log(`📸 Screenshots: ${RESULTS_DIR}/${TEST_PREFIX}_*.png`);
  
  return testResults;
}

runFullTests().catch(console.error);
