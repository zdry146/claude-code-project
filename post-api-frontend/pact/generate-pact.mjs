/**
 * Pact Consumer Contract Definition
 * Defines the expected API contract from the frontend's perspective.
 * Generates pact.json for provider verification.
 * Pure Node.js - no external dependencies.
 */

import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const CONTRACT_FILE = join(__dirname, 'pacts', 'post-api-frontend-post-api.json');
const API_BASE = 'http://localhost:8083';

const contract = {
  consumer: { name: 'post-api-frontend' },
  provider: { name: 'post-api' },
  interactions: [
    {
      type: 'Synchronous/HTTP',
      description: 'GET /api/posts/published — list published posts (paginated)',
      request: { method: 'GET', path: '/api/posts/published', query: { page: '0', size: '10' } },
      response: {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: {
          code: 200,
          message: 'success',
          data: {
            content: [{ id: 1, title: 'Post Title', content: 'Content', authorName: 'Author', 
              coverImage: null, viewCount: 0, likeCount: 0,
              isPublished: true, createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00' }],
            page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true
          }
        }
      }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'POST /api/posts — create new post',
      request: { 
        method: 'POST', path: '/api/posts',
        headers: { 'Content-Type': 'application/json' },
        body: { title: 'My Post', content: 'Content', authorName: 'Author' }
      },
      response: {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: { code: 200, message: 'success', data: { id: 1 } }
      }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'GET /api/posts/{id} — get post by id',
      request: { method: 'GET', path: '/api/posts/1' },
      response: {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: { code: 200, data: { id: 1, title: 'Post', authorName: 'Author' } }
      }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'PUT /api/posts/{id} — update post',
      request: {
        method: 'PUT', path: '/api/posts/1',
        headers: { 'Content-Type': 'application/json' },
        body: { title: 'Updated', content: 'Updated' }
      },
      response: { status: 200, body: { code: 200, message: 'success' } }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'DELETE /api/posts/{id} — delete post',
      request: { method: 'DELETE', path: '/api/posts/1' },
      response: { status: 200, body: { code: 200 } }
    },
    {
      type: 'Synchronous/HTTP',
      type: 'Synchronous/HTTP',
      description: 'POST /api/posts/{id}/like — like post',
      request: { method: 'POST', path: '/api/posts/1/like' },
      response: { status: 200, body: { code: 200, data: { likeCount: 1 } } }
    },
    {
      type: 'Synchronous/HTTP',
      type: 'Synchronous/HTTP',
      description: 'POST /api/posts/{id}/toggle-publish — toggle publish',
      request: { method: 'POST', path: '/api/posts/1/toggle-publish' },
      response: { status: 200, body: { code: 200, data: { isPublished: true } } }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'GET /api/posts/search — search posts',
      request: { method: 'GET', path: '/api/posts/search', query: { keyword: 'test' } },
      response: {
        status: 200,
        body: { code: 200, data: { content: [{ id: 1 }] } }
      }
    },
    {
      type: 'Synchronous/HTTP',
      description: 'GET /api/posts/all — admin list all posts',
      request: { method: 'GET', path: '/api/posts/all', query: { page: '0', size: '10' } },
      response: {
        status: 200,
        body: { code: 200, data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } }
      }
    }
  ],
  metadata: {
    pactSpecification: { version: '4.0' },
    pactfile: { tool: 'pact-consumer-test' }
  }
};

mkdirSync(dirname(CONTRACT_FILE), { recursive: true });
writeFileSync(CONTRACT_FILE, JSON.stringify(contract, null, 2));
console.log(`\n=== Pact Consumer Contract Generated ===`);
console.log(`✅ File: ${CONTRACT_FILE}`);
console.log(`✅ Consumer: ${contract.consumer.name}`);
console.log(`✅ Provider: ${contract.provider.name}`);
console.log(`✅ Interactions: ${contract.interactions.length}`);
contract.interactions.forEach(i => console.log(`   ${i.request.method} ${i.request.path} — ${i.description}`));
console.log();
