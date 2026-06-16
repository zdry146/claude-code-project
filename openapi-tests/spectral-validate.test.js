/**
 * OpenAPI Spec Validation — real Spectral 6 + Jest (CommonJS)
 * Uses @stoplight/spectral-core to lint openapi-spec.json with custom rules.
 */

const { Spectral, Document } = require('@stoplight/spectral-core');
const { oas } = require('@stoplight/spectral-rulesets');
const { truthy, pattern } = require('@stoplight/spectral-functions');
const { parseJson } = require('@stoplight/spectral-parsers');
const { Json } = require('@stoplight/spectral-parsers');
const { readFileSync } = require('fs');

const specRaw = readFileSync('./openapi-spec.json', 'utf8');

// Custom ruleset (Spectral 6.x format)
const ruleset = {
  extends: [oas],
  rules: {
    // All POST/PUT success responses should have a JSON schema
    'post-api-response-schema': {
      description: 'Success responses should have a JSON schema defined',
      severity: 'warn',
      given: [
        '$.paths.*.post.responses[?(@property == "200" || @property == "201")].content',
        '$.paths.*.put.responses[?(@property == "200" || @property == "201")].content',
      ],
      then: [
        { field: 'application/json.schema', function: truthy },
      ],
    },

    // Pagination params for list endpoints
    'post-api-pagination-params': {
      description: 'List endpoints should have page query param',
      severity: 'warn',
      given: [
        '$.paths["/api/posts/published"].get.parameters',
        '$.paths["/api/posts/all"].get.parameters',
      ],
      then: [
        {
          field: 'name',
          function: pattern,
          functionOptions: { match: 'page' },
        },
      ],
    },

    // Standard error responses documented
    'post-api-error-responses': {
      description: 'Operations should document 4xx error responses',
      severity: 'warn',
      given: '$.paths.*.[get,post,put,delete].responses',
      then: [
        { field: '400', function: truthy },
      ],
    },

    // ApiResult wrapper
    'post-api-result-wrapper': {
      description: 'Response schemas should use ApiResult wrapper (have .code field)',
      severity: 'info',
      message: 'Consider using ApiResult<T> wrapper for consistent response shape',
      given: '$.paths.*.[get,post,put,delete].responses.200.content."application/json".schema',
      then: [
        { field: 'properties.code', function: truthy },
      ],
    },

    // Info completeness
    'info-description': {
      description: 'OpenAPI info block should have a description',
      severity: 'info',
      given: '$.info',
      then: [
        { field: 'description', function: truthy },
      ],
    },
  },
};

describe('OpenAPI Spec Validation (real Spectral)', () => {
  let spectral;
  let document;

  beforeAll(async () => {
    spectral = new Spectral();
    spectral.setRuleset(ruleset);
    document = new Document(specRaw, Json, 'openapi-spec.json');
  });

  test('lints the spec without errors', async () => {
    const results = await spectral.run(document);
    const errors = results.filter(r => r.severity === 0);
    const warnings = results.filter(r => r.severity === 1);
    const infos = results.filter(r => r.severity === 2);

    console.log(`\n📊 Spectral: ${errors.length} errors, ${warnings.length} warnings, ${infos.length} info\n`);
    if (errors.length > 0) {
      console.log('❌ Errors:');
      errors.forEach(e => console.log(`  [${e.code}] ${e.message} — ${e.path.join('.')}`));
    }
    if (warnings.length > 0) {
      console.log('⚠️  Warnings:');
      warnings.forEach(w => console.log(`  [${w.code}] ${w.message} — ${w.path.join('.')}`));
    }
    if (infos.length > 0) {
      console.log('💡 Info:');
      infos.forEach(i => console.log(`  [${i.code}] ${i.message} — ${i.path.join('.')}`));
    }
    expect(errors.length).toBe(0);
  });

  test('spec has expected endpoints', () => {
    const spec = JSON.parse(specRaw);
    const paths = Object.keys(spec.paths || {});
    console.log(`  Found ${paths.length} endpoints: ${paths.join(', ')}`);
    expect(paths).toContain('/api/posts');
    expect(paths).toContain('/api/posts/published');
    expect(paths).toContain('/api/posts/{id}');
    expect(paths).toContain('/api/posts/search');
  });

  test('spec has component schemas', () => {
    const spec = JSON.parse(specRaw);
    const schemas = Object.keys(spec.components?.schemas || {});
    console.log(`  Component schemas: ${schemas.join(', ')}`);
    expect(schemas.length).toBeGreaterThan(0);
  });

  test('all operations have operationId', () => {
    const spec = JSON.parse(specRaw);
    const missing = [];
    for (const [path, methods] of Object.entries(spec.paths || {})) {
      for (const [method, op] of Object.entries(methods)) {
        if (!op.operationId) missing.push(`${method.toUpperCase()} ${path}`);
      }
    }
    if (missing.length > 0) console.log(`  Missing operationIds: ${missing.join(', ')}`);
    expect(missing).toEqual([]);
  });

  test('ApiResult wrapper is used in 2xx responses', () => {
    const spec = JSON.parse(specRaw);
    const withoutWrapper = [];
    for (const [path, methods] of Object.entries(spec.paths || {})) {
      for (const [method, op] of Object.entries(methods)) {
        const success = op.responses?.['200'] || op.responses?.['201'];
        if (success?.content?.['application/json']?.schema) {
          const hasCode = success.content['application/json'].schema?.properties?.code;
          if (!hasCode) withoutWrapper.push(`${method.toUpperCase()} ${path}`);
        }
      }
    }
    if (withoutWrapper.length > 0) {
      console.log(`  Without ApiResult wrapper: ${withoutWrapper.join(', ')}`);
    }
    expect(withoutWrapper).toEqual([]);
  });
});
