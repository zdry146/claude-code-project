# post-api (claude-code-project) - Agent Instructions

## Jenkins Integration

The three Jenkins jobs (`post-api-ci`, `-cd`, `-cicd`) are
**Pipeline script** (NOT "from SCM") wrapping
[`jenkins/wrappers/git-fallback-wrapper.groovy`](jenkins/wrappers/git-fallback-wrapper.groovy).

### Why not "Pipeline from SCM"?

When Jenkins loads a pipeline from SCM, it has to fetch the Jenkinsfile
from GitHub at job-start time. If GitHub is down, the pipeline can't
even start — there's no chance to fall back. The wrapper inverts
this: it runs as a Groovy script embedded directly in the job config,
tries to `git clone` the repo from GitHub first (30s timeout), and
falls back to Gitee (SSH) on failure. After the checkout, it
`evaluate()`s the real pipeline script at
`jenkins/combined-pipeline-scm.groovy` from the workspace.

The three things the wrapper handles that naive "Pipeline from SCM"
can't:

1. **GitHub→Gitee fallback** — `git clone` with timeout, then fallback
   to `git@gitee.com:zdry146/claude-code-project.git`. Uses SSH key
   `~/.ssh/gitee_key` (separate from `~/.ssh/github_key`) configured
   in `~/.ssh/config` per-host.
2. **Move cloned files to workspace root** — inner Jenkinsfile expects
   `pom.xml` in the workspace root, not a subdirectory. Uses `bash`
   (not `dash`) and `cp -a .cloned-tmp/. .` to overwrite.
3. **`agent any` → `agent none` substitution** — without this, the
   inner pipeline allocates a fresh empty workspace (`cicd@2`),
   ignoring the wrapper's already-cloned repo. Substituting
   `agent none` makes the inner stages reuse the outer `node {}`
   context.

### Branch: `master`

This repo's default branch is `master` (not `main`). The wrapper's
`git clone -b master` calls reflect this.

### Required Jenkins credentials

Already exist on the Jenkins instance (no per-project setup needed):
- `aliyun-docker-login` (username/password)
- `db-password` (Secret text)
- `git-cred` (username/password, used by the Jenkinsfile for the
  frontend submodule clone)

### Script approval caveat

Whenever the wrapper's text changes (e.g. SSH URL, branch name,
agent substitution logic), Jenkins requires manual approval via
`/scriptApproval` (UI) before builds will run. The Stapler-bound
`approveScript` RPC returns HTTP 200 but does NOT actually approve —
always do this in the UI.

### Reference: bringing up the wrapper-based jobs on a new host

```bash
# 1. Set up SSH keys for GitHub + Gitee on the Jenkins host
ssh-keygen -t ed25519 -C "openclaw@local" -f ~/.ssh/github_key
ssh-keygen -t ed25519 -C "openclaw@local-gitee" -f ~/.ssh/gitee_key
# Paste github_key.pub → github.com/.../keys
# Paste gitee_key.pub  → gitee.com/.../ssh_keys
cat >> ~/.ssh/config <<'EOF'
Host github.com
    IdentityFile ~/.ssh/github_key
    User git
    IdentitiesOnly yes
Host gitee.com
    IdentityFile ~/.ssh/gitee_key
    User git
    IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config

# 2. Create the 3 jobs (idempotent, auto-converts legacy Pipeline from SCM)
export JENKINS_USER=admin JENKINS_TOKEN=***
python3 scripts/jenkins-create-combined-jobs.py

# 3. Trigger once → approve the wrapper in /scriptApproval UI (one-time)
#    Manage Jenkins → In-process Script Approval → Approve
```

### Editing the wrapper

After editing `jenkins/wrappers/git-fallback-wrapper.groovy`:

1. Run `scripts/jenkins-create-combined-jobs.py` to push the new
   wrapper into all three job configs.
2. Trigger one build → the script will fail with
   `UnapprovedUsageException` until you Approve in
   `/scriptApproval` (one-time per unique script text).
3. Re-trigger; subsequent builds run without re-approval until the
   wrapper text changes again.

### Editing the pipeline

Edit `jenkins/combined-pipeline-scm.groovy`, commit, push to
`master`. No manual sync needed — the wrapper will `git clone` the
new commit on the next build (regardless of GitHub or Gitee
fallback path).

## Key Files

- `pom.xml` - Maven config (Spring Boot 3.x, Java 21)
- `Dockerfile` - Container image
- `jenkins/combined-pipeline-scm.groovy` - The single pipeline script
  (used by all 3 Jenkins jobs via `evaluate()` from the wrapper)
- `jenkins/wrappers/git-fallback-wrapper.groovy` - GitHub→Gitee fallback
  wrapper embedded in each job config; loads
  `combined-pipeline-scm.groovy` after a successful git clone
- `jenkins/job-config.xml` - Reference job config template (used by
  setup scripts)
- `scripts/jenkins-create-combined-jobs.py` - Idempotent helper to
  create or refresh the 3 Jenkins jobs (uses the wrapper script)
- `k8s/` - Kubernetes manifests
- `scripts/init-db.sh` - One-time DB initialization
