/**
 * create-postman-pr.mjs
 *
 * Exports a Postman collection and opens a GitHub PR with the collection JSON.
 *
 * Required env vars:
 *   POSTMAN_API_KEY - Your Postman API key
 *   GITHUB_TOKEN    - GitHub token with access to the repo
 *
 * Optional env vars:
 *   COLLECTION_ID   - Defaults to the User Access Control API collection ID
 *   BRANCH_NAME     - Defaults to postman/user-access-control-tests
 *   BASE_BRANCH     - Defaults to the repo's default branch
 *   FILE_PATH       - Defaults to postman/UserAccessControlAPI.postman_collection.json
 */

const POSTMAN_API_KEY = process.env.POSTMAN_API_KEY;
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;

const OWNER = "vtsyryuk";
const REPO = "UserAccessControl";

const COLLECTION_ID =
  process.env.COLLECTION_ID ||
  "56353536-4d97b184-81af-4069-aa85-7fe61b680b7d";

const BRANCH_NAME =
  process.env.BRANCH_NAME || "postman/user-access-control-tests";

const FILE_PATH =
  process.env.FILE_PATH || "postman/UserAccessControlAPI.postman_collection.json";

const COMMIT_MESSAGE = "Add Postman API tests";
const PR_TITLE = "Add Postman API tests";
const PR_BODY = `
Adds the Postman collection for the User Access Control API.

Included tests cover:
- Listing resources
- Acquiring access as Alice, Bob, and Dave
- Verifying Bob and Dave are denied write access
- Simulating access
- Releasing access by key
- Releasing access by lease ID
`.trim();

if (!POSTMAN_API_KEY) {
  throw new Error("Missing POSTMAN_API_KEY environment variable.");
}

if (!GITHUB_TOKEN) {
  throw new Error("Missing GITHUB_TOKEN environment variable.");
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);

  const text = await response.text();
  let body = null;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof body === "string" ? body : JSON.stringify(body, null, 2);

    throw new Error(
      `Request failed: ${options.method || "GET"} ${url}\n` +
        `Status: ${response.status}\n` +
        `Response: ${message}`
    );
  }

  return body;
}

async function getPostmanCollection() {
  console.log("Exporting Postman collection...");

  const data = await requestJson(
    `https://api.getpostman.com/collections/${COLLECTION_ID}`,
    {
      headers: {
        "X-Api-Key": POSTMAN_API_KEY,
      },
    }
  );

  if (!data.collection) {
    throw new Error("Postman API response did not include a collection object.");
  }

  return data.collection;
}

function githubHeaders() {
  return {
    Accept: "application/vnd.github+json",
    Authorization: `Bearer ${GITHUB_TOKEN}`,
    "X-GitHub-Api-Version": "2022-11-28",
    "Content-Type": "application/json",
  };
}

async function getRepo() {
  return requestJson(`https://api.github.com/repos/${OWNER}/${REPO}`, {
    headers: githubHeaders(),
  });
}

async function getBranchSha(branchName) {
  const ref = await requestJson(
    `https://api.github.com/repos/${OWNER}/${REPO}/git/ref/heads/${encodeURIComponent(
      branchName
    )}`,
    {
      headers: githubHeaders(),
    }
  );

  return ref.object.sha;
}

async function branchExists(branchName) {
  const url = `https://api.github.com/repos/${OWNER}/${REPO}/git/ref/heads/${encodeURIComponent(
    branchName
  )}`;

  const response = await fetch(url, {
    headers: githubHeaders(),
  });

  if (response.status === 404) {
    return false;
  }

  if (!response.ok) {
    const body = await response.text();
    throw new Error(
      `Failed checking branch existence: ${response.status}\n${body}`
    );
  }

  return true;
}

async function createBranch(branchName, baseSha) {
  console.log(`Creating branch: ${branchName}`);

  return requestJson(`https://api.github.com/repos/${OWNER}/${REPO}/git/refs`, {
    method: "POST",
    headers: githubHeaders(),
    body: JSON.stringify({
      ref: `refs/heads/${branchName}`,
      sha: baseSha,
    }),
  });
}

async function getExistingFileSha(path, branchName) {
  const url =
    `https://api.github.com/repos/${OWNER}/${REPO}/contents/` +
    `${encodeURIComponent(path).replaceAll("%2F", "/")}?ref=${encodeURIComponent(
      branchName
    )}`;

  const response = await fetch(url, {
    headers: githubHeaders(),
  });

  if (response.status === 404) {
    return null;
  }

  const body = await response.json();

  if (!response.ok) {
    throw new Error(
      `Failed checking existing file: ${response.status}\n${JSON.stringify(
        body,
        null,
        2
      )}`
    );
  }

  return body.sha;
}

async function putFile({ path, branchName, content }) {
  console.log(`Committing collection to ${path} on ${branchName}...`);

  const existingSha = await getExistingFileSha(path, branchName);

  const encodedContent = Buffer.from(content, "utf8").toString("base64");

  const body = {
    message: COMMIT_MESSAGE,
    content: encodedContent,
    branch: branchName,
  };

  if (existingSha) {
    body.sha = existingSha;
  }

  return requestJson(
    `https://api.github.com/repos/${OWNER}/${REPO}/contents/${encodeURIComponent(
      path
    ).replaceAll("%2F", "/")}`,
    {
      method: "PUT",
      headers: githubHeaders(),
      body: JSON.stringify(body),
    }
  );
}

async function findExistingPullRequest(branchName, baseBranch) {
  const prs = await requestJson(
    `https://api.github.com/repos/${OWNER}/${REPO}/pulls?state=open&head=${OWNER}:${encodeURIComponent(
      branchName
    )}&base=${encodeURIComponent(baseBranch)}`,
    {
      headers: githubHeaders(),
    }
  );

  return prs[0] || null;
}

async function createPullRequest(branchName, baseBranch) {
  const existingPr = await findExistingPullRequest(branchName, baseBranch);

  if (existingPr) {
    console.log(`PR already exists: ${existingPr.html_url}`);
    return existingPr;
  }

  console.log("Creating pull request...");

  return requestJson(`https://api.github.com/repos/${OWNER}/${REPO}/pulls`, {
    method: "POST",
    headers: githubHeaders(),
    body: JSON.stringify({
      title: PR_TITLE,
      head: branchName,
      base: baseBranch,
      body: PR_BODY,
    }),
  });
}

async function main() {
  const collection = await getPostmanCollection();

  const repo = await getRepo();
  const baseBranch = process.env.BASE_BRANCH || repo.default_branch;

  console.log(`Using base branch: ${baseBranch}`);

  const baseSha = await getBranchSha(baseBranch);

  const exists = await branchExists(BRANCH_NAME);

  if (!exists) {
    await createBranch(BRANCH_NAME, baseSha);
  } else {
    console.log(`Branch already exists: ${BRANCH_NAME}`);
  }

  const collectionJson = JSON.stringify(collection, null, 2) + "\n";

  await putFile({
    path: FILE_PATH,
    branchName: BRANCH_NAME,
    content: collectionJson,
  });

  const pr = await createPullRequest(BRANCH_NAME, baseBranch);

  console.log("");
  console.log("Done.");
  console.log(`Pull request: ${pr.html_url}`);
}

main().catch((error) => {
  console.error("");
  console.error("Failed to create PR:");
  console.error(error.message);
  process.exit(1);
});
