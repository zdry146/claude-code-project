import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'test-results/report' }],
    ['list']
  ],
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:8083',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { 
        ...devices['Desktop Chrome'],
        launchOptions: { executablePath: '/snap/bin/chromium' }
      },
    },
  ],
  webServer: {
    command: 'echo "Using external API at localhost:8083"',
    port: 8083,
    reuseExistingServer: true,
    timeout: 5000,
  },
});
