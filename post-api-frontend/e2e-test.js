import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Test configuration
const API_BASE = 'http://localhost:8080/api';
const FRONTEND_URL = 'http://localhost:5173';
const RESULTS_DIR = path.join(__dirname, 'test-results');

// Ensure results directory exists
if (!fs.existsSync(RESULTS_DIR)) {
  fs.mkdirSync(RESULTS_DIR, { recursive: true });
}

// Helper function to make HTTP requests
function httpRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const reqOptions = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method: options.method || 'GET',
      headers: options.headers || {}
    };

    const req = http.request(reqOptions, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, data: data });
        }
      });
    });

    req.on('error', reject);
    if (options.body) req.write(options.body);
    req.end();
  });
}

// Test results collector
const testResults = {
  timestamp: new Date().toISOString(),
  tests: []
};

function addTest(name, passed, details = '') {
  testResults.tests.push({ name, passed, details, time: new Date().toISOString() });
  console.log(`${passed ? '✅' : '❌'} ${name}${details ? ': ' + details : ''}`);
}

// Run tests
async function runTests() {
  console.log('🔄 Starting E2E Tests...\n');
  console.log('='.repeat(50));
  console.log(`Backend API: ${API_BASE}`);
  console.log(`Frontend: ${FRONTEND_URL}`);
  console.log('='.repeat(50) + '\n');

  // Test 1: Check backend is running
  try {
    const res = await httpRequest(`${API_BASE}/posts/published`);
    addTest('Backend API is running', res.status === 200, `Status: ${res.status}`);
  } catch (e) {
    addTest('Backend API is running', false, e.message);
  }

  // Test 2: Get posts list (published)
  try {
    const res = await httpRequest(`${API_BASE}/posts/published`);
    const count = res.data.data?.content?.length || 0;
    addTest('Get published posts list', res.status === 200, `Found ${count} posts`);
  } catch (e) {
    addTest('Get published posts list', false, e.message);
  }

  // Test 3: Create a new post
  let createdPostId = null;
  try {
    const postData = {
      title: 'E2E Test Post',
      content: 'This is a test post created by automated E2E testing',
      authorName: 'E2E Tester'
    };
    const res = await httpRequest(`${API_BASE}/posts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(postData)
    });
    if (res.status === 200 && res.data.data) {
      createdPostId = res.data.data.id;
      addTest('Create new post', true, `Post ID: ${createdPostId}`);
    } else {
      addTest('Create new post', false, `Status: ${res.status}`);
    }
  } catch (e) {
    addTest('Create new post', false, e.message);
  }

  // Test 4: Get single post
  if (createdPostId) {
    try {
      const res = await httpRequest(`${API_BASE}/posts/${createdPostId}`);
      const found = res.data.data && res.data.data.id === createdPostId;
      addTest('Get single post by ID', found, `Title: ${res.data.data?.title}`);
    } catch (e) {
      addTest('Get single post by ID', false, e.message);
    }
  }

  // Test 5: Update post
  if (createdPostId) {
    try {
      const updateData = {
        title: 'E2E Test Post (Updated)',
        content: 'This post was updated by E2E test',
        authorName: 'E2E Tester'
      };
      const res = await httpRequest(`${API_BASE}/posts/${createdPostId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updateData)
      });
      const updated = res.data.data && res.data.data.title === 'E2E Test Post (Updated)';
      addTest('Update post', updated, updated ? 'Title updated successfully' : 'Update failed');
    } catch (e) {
      addTest('Update post', false, e.message);
    }
  }

  // Test 6: Search posts
  try {
    const res = await httpRequest(`${API_BASE}/posts/search?keyword=E2E`);
    const found = res.data.data && res.data.data.content.length > 0;
    addTest('Search posts', found, `Found ${res.data.data?.content?.length || 0} matching posts`);
  } catch (e) {
    addTest('Search posts', false, e.message);
  }

  // Test 7: Toggle publish status
  if (createdPostId) {
    try {
      const res = await httpRequest(`${API_BASE}/posts/${createdPostId}/toggle-publish`, {
        method: 'POST'
      });
      const toggled = res.data.data && res.data.data.isPublished === true;
      addTest('Toggle publish status', toggled, toggled ? 'Post is now published' : 'Toggle failed');
    } catch (e) {
      addTest('Toggle publish status', false, e.message);
    }
  }

  // Test 8: Like post
  if (createdPostId) {
    try {
      const res = await httpRequest(`${API_BASE}/posts/${createdPostId}/like`, {
        method: 'POST'
      });
      const liked = res.data.data && res.data.data.likeCount >= 1;
      addTest('Like post', liked, `Like count: ${res.data.data?.likeCount}`);
    } catch (e) {
      addTest('Like post', false, e.message);
    }
  }

  // Test 9: Frontend is accessible
  try {
    const res = await httpRequest(FRONTEND_URL);
    const hasContent = res.status === 200 && res.data.includes('root');
    addTest('Frontend is accessible', hasContent, `Status: ${res.status}`);
  } catch (e) {
    addTest('Frontend is accessible', false, e.message);
  }

  // Test 10: Delete post (soft delete)
  if (createdPostId) {
    try {
      const res = await httpRequest(`${API_BASE}/posts/${createdPostId}`, {
        method: 'DELETE'
      });
      addTest('Delete post (soft delete)', res.status === 200, res.data.message);
    } catch (e) {
      addTest('Delete post (soft delete)', false, e.message);
    }
  }

  // Summary
  console.log('\n' + '='.repeat(50));
  const passed = testResults.tests.filter(t => t.passed).length;
  const failed = testResults.tests.filter(t => !t.passed).length;
  console.log(`\n📊 Test Summary: ${passed} passed, ${failed} failed out of ${testResults.tests.length} tests`);
  
  // Save results
  const resultsFile = path.join(RESULTS_DIR, 'e2e-test-results.json');
  fs.writeFileSync(resultsFile, JSON.stringify(testResults, null, 2));
  console.log(`\n📁 Results saved to: ${resultsFile}`);

  return testResults;
}

runTests().catch(console.error);
