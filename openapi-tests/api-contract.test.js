const axios = require("axios");
const path = require("path");
const $RefParser = require("@apidevtools/json-schema-ref-parser");
const Ajv = require("ajv");
const addFormats = require("ajv-formats");

const BASE_URL = "http://localhost:8083";

let spec;
let ajv;
let createdPostId;

beforeAll(async () => {
  spec = await $RefParser.dereference(
    path.join(__dirname, "openapi-spec.json")
  );

  ajv = new Ajv({ allErrors: true, strict: false });
  addFormats(ajv);
  ajv.addFormat("int32", /^-?\d+$/);
  ajv.addFormat("int64", /^-?\d+$/);
  ajv.addFormat("date-time", (s) => {
    if (typeof s !== "string") return false;
    return !isNaN(Date.parse(s));
  });

  for (const [name, schema] of Object.entries(spec.components.schemas)) {
    ajv.addSchema(makeNullable(schema), `#/components/schemas/${name}`);
  }
}, 30000);

function makeNullable(schema) {
  if (!schema || typeof schema !== "object") return schema;
  const copy = JSON.parse(JSON.stringify(schema));
  function walk(s) {
    if (!s || typeof s !== "object") return;
    if (s.properties) {
      for (const prop of Object.values(s.properties)) {
        if (prop.type && !Array.isArray(prop.type)) {
          prop.type = [prop.type, "null"];
        }
        walk(prop);
      }
    }
    if (s.items) walk(s.items);
    if (s.oneOf) s.oneOf.forEach(walk);
    if (s.anyOf) s.anyOf.forEach(walk);
    if (s.allOf) s.allOf.forEach(walk);
  }
  walk(copy);
  return copy;
}

afterAll(async () => {
  if (createdPostId) {
    try {
      await axios.delete(`${BASE_URL}/api/posts/${createdPostId}`);
    } catch (e) {
      // ignore cleanup errors
    }
  }
});

function validate(schemaRef, data) {
  const validateFn = ajv.getSchema(`#/components/schemas/${schemaRef}`);
  if (!validateFn) {
    throw new Error(`Schema "${schemaRef}" not found in compiled schemas`);
  }
  return { valid: validateFn(data), errors: validateFn.errors };
}

function assertValid(schemaRef, data) {
  const { valid, errors } = validate(schemaRef, data);
  if (!valid) {
    console.error(
      "Schema validation errors:",
      JSON.stringify(errors, null, 2)
    );
  }
  expect(valid).toBe(true);
}

describe("Post CRUD API Contract Tests", () => {
  test(
    "GET /api/posts/published - returns paginated posts with content array",
    async () => {
      const res = await axios.get(`${BASE_URL}/api/posts/published`);
      expect(res.status).toBe(200);
      expect(res.data).toHaveProperty("code");
      expect(res.data).toHaveProperty("message");
      expect(res.data.data).toHaveProperty("content");
      expect(Array.isArray(res.data.data.content)).toBe(true);
      expect(res.data.data).toHaveProperty("page");
      expect(res.data.data).toHaveProperty("size");
      expect(res.data.data).toHaveProperty("totalElements");
      expect(res.data.data).toHaveProperty("totalPages");
      assertValid("ApiResultPageResponsePostResponse", res.data);
    },
    15000
  );

  test("POST /api/posts - creates a post", async () => {
    const newPost = {
      title: "Contract Test Post",
      content: "Created by api contract test",
      authorName: "Tester",
    };

    const res = await axios.post(`${BASE_URL}/api/posts`, newPost);
    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty("code");
    expect(res.data).toHaveProperty("message");
    expect(res.data.data).toHaveProperty("id");
    expect(res.data.data.title).toBe(newPost.title);
    expect(res.data.data.authorName).toBe(newPost.authorName);

    createdPostId = res.data.data.id;

    assertValid("ApiResultPostResponse", res.data);
  }, 15000);

  test("GET /api/posts/{id} - returns a single post", async () => {
    const newPost = {
      title: "Contract Test Get Post",
      content: "For GET test",
      authorName: "Tester",
    };

    const createRes = await axios.post(`${BASE_URL}/api/posts`, newPost);
    const postId = createRes.data.data.id;

    try {
      const res = await axios.get(`${BASE_URL}/api/posts/${postId}`);
      expect(res.status).toBe(200);
      expect(res.data).toHaveProperty("code");
      expect(res.data).toHaveProperty("message");
      expect(res.data.data).toHaveProperty("id", postId);
      expect(res.data.data).toHaveProperty("title");
      expect(res.data.data).toHaveProperty("content");
      expect(res.data.data).toHaveProperty("authorName");
      expect(res.data.data).toHaveProperty("viewCount");
      expect(res.data.data).toHaveProperty("likeCount");

      assertValid("ApiResultPostResponse", res.data);
    } finally {
      try {
        await axios.delete(`${BASE_URL}/api/posts/${postId}`);
      } catch (e) {}
    }
  }, 15000);

  test("DELETE /api/posts/{id} - deletes a post", async () => {
    const newPost = {
      title: "Contract Test Delete Post",
      content: "For DELETE test",
      authorName: "Tester",
    };

    const createRes = await axios.post(`${BASE_URL}/api/posts`, newPost);
    const postId = createRes.data.data.id;

    const res = await axios.delete(`${BASE_URL}/api/posts/${postId}`);
    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty("code");
    expect(res.data).toHaveProperty("message");

    // Verify post is gone (server returns 404)
    try {
      const getRes = await axios.get(`${BASE_URL}/api/posts/${postId}`);
      expect(getRes.data.code).not.toBe(0);
    } catch (e) {
      expect(e.response.status).toBe(404);
    }
  }, 15000);

  test("GET /api/posts/search?keyword=test - searches posts", async () => {
    const res = await axios.get(`${BASE_URL}/api/posts/search`, {
      params: { keyword: "test" },
    });
    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty("code");
    expect(res.data).toHaveProperty("message");
    expect(res.data.data).toHaveProperty("content");
    expect(Array.isArray(res.data.data.content)).toBe(true);
    expect(res.data.data).toHaveProperty("page");
    expect(res.data.data).toHaveProperty("size");

    assertValid("ApiResultPageResponsePostResponse", res.data);
  }, 15000);
});
