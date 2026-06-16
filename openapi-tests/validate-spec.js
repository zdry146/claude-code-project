const { execSync } = require("child_process");
const path = require("path");

const specPath = path.join(__dirname, "openapi-spec.json");
const rulesetPath = path.join(__dirname, ".spectral.yaml");

console.log("\n=== OpenAPI Spec Validation (Spectral) ===\n");

try {
  const output = execSync(
    `npx spectral lint "${specPath}" --ruleset "${rulesetPath}"`,
    { encoding: "utf-8", stdio: "pipe" }
  );
  console.log(output);
  console.log("Spec validation passed.");
  process.exit(0);
} catch (e) {
  const out = e.stdout || "";
  const err = e.stderr || "";
  if (out) console.log(out);
  if (err && err !== out) console.error(err);

  const combined = out + err;
  if (combined.toLowerCase().includes("error")) {
    console.error("Spec validation FAILED with errors.");
    process.exit(1);
  }

  console.log("Spec validation passed (warnings only).");
  process.exit(0);
}
