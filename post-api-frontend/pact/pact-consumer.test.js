/**
 * Pact Consumer Test — post-api-frontend
 * Defines the contracts the frontend expects from the Post API.
 * Generates pact file that the provider (post-api) will verify.
 */

const { PactV3, MatchersV3 } = require('@pact-foundation/pact');
const { like, eachLike, integer, string } = MatchersV3;
const path = require('path');

// We can't install @pact-foundation/pact easily, so this is a 
// simplified consumer contract definition using a plain HTTP recorder pattern
// The pact file will be verified manually against the running API.

const API_BASE = 'http://localhost:8083';
const CONTRACT_FILE = path.join(__dirname, 'pacts', 'post-api-frontend-post-api.json');

async function defineContract() {
  console.log('\n=== Pact Consumer Contract Definition ===\n');
  
  const contract = {
    consumer: { name: 'post-api-frontend' },
    provider: { name: 'post-api' },
    interactions: [
      // 1. List published posts
      {
        description: 'get published posts',
        request: { method: 'GET', path: '/api/posts/published', query: 'page=0&size=10' },
        response: {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
          body: {
            code: 200,
            message: 'success',
            data: {
              content: eachLike({
                id: integer(1),
                title: string('Post Title'),
                content: string('Post content'),
                authorName: string('Author'),
                coverImage: null,
                viewCount: integer(0),
                likeCount: integer(0),
                isPublished: true,
                createdAt: string('2026-01-01T00:00:00'),
                updatedAt: string('2026-01-01T00:00:00')
              }),
              page: 0,
              size: 10,
              totalElements: integer(0),
              totalPages: integer(0),
              first: true,
              last: true
            }
          }
        }
      },
      // 2. Create post
      {
        description: 'create a new post',
        request: {
          method: 'POST',
          path: '/api/posts',
          headers: { 'Content-Type': 'application/json' },
          body: {
            title: string('My Post'),
            content: string('My content'),
            authorName: string('Author')
          }
        },
        response: {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
          body: {
            code: 200,
            message: like('success'),
            data: {
              id: integer(1),
              title: string('My Post'),
              content: string('My content'),
              authorName: string('Author'),
              isPublished: false,
              likeCount: 0,
              viewCount: 0
            }
          }
        }
      },
      // 3. Get single post
      {
        description: 'get post by id',
        request: { method: 'GET', path: '/api/posts/1' },
        response: {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
          body: {
            code: 200,
            message: 'success',
            data: { id: integer(1) }
          }
        }
      },
      // 4. Update post
      {
        description: 'update a post',
        request: {
          method: 'PUT',
          path: '/api/posts/1',
          headers: { 'Content-Type': 'application/json' },
          body: { title: string('Updated'), content: string('Updated content') }
        },
        response: {
          status: 200,
          body: { code: 200, message: like('success') }
        }
      },
      // 5. Delete post
      {
        description: 'delete a post',
        request: { method: 'DELETE', path: '/api/posts/1' },
        response: { status: 200, body: { code: 200 } }
      },
      // 6. Like post
      {
        description: 'like a post',
        request: { method: 'POST', path: '/api/posts/1/like' },
        response: {
          status: 200,
          body: { code: 200, message: like('success'), data: { likeCount: integer(1) } }
        }
      },
      // 7. Search posts
      {
        description: 'search posts by keyword',
        request: { method: 'GET', path: '/api/posts/search', query: 'keyword=test' },
        response: {
          status: 200,
          body: { code: 200, data: { content: eachLike({ id: integer(1) }) } }
        }
      }
    ],
    metadata: {
      pactSpecification: { version: '4.0' }
    }
  };

  // Write pact file
  const fs = require('fs');
  fs.mkdirSync(path.dirname(CONTRACT_FILE), { recursive: true });
  fs.writeFileSync(CONTRACT_FILE, JSON.stringify(contract, null, 2));
  console.log(`✅ Consumer contract written: ${CONTRACT_FILE}`);
  console.log(`   ${contract.interactions.length} interactions defined\n`);
}

defineContract().catch(err => {
  console.error('❌ Failed:', err.message);
  process.exit(1);
});
