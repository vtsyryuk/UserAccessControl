import { expect, test } from "@playwright/test";

test.describe.configure({ mode: "serial" });

const paymentResource = "config/payment.yml";
const reportResource = "reports/monthly.csv";

async function releaseByKey(request, key) {
  await request.post(`/api/release?key=${encodeURIComponent(key)}`);
}

test.beforeEach(async ({ request }) => {
  await releaseByKey(request, paymentResource);
  await releaseByKey(request, reportResource);
});

test.afterEach(async ({ request }) => {
  await releaseByKey(request, paymentResource);
  await releaseByKey(request, reportResource);
});

test("loads the cloud demo UI and resource repository", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveTitle(/UserAccessControl demo/);
  await expect(page.getByRole("heading", { name: "UserAccessControl demo" })).toBeVisible();
  await expect(page.locator("#resources")).toContainText(paymentResource);
  await expect(page.locator("#resources")).toContainText(reportResource);
  await expect(page.locator("#status")).toContainText("Loaded 4 resources");
});

test("acquires and releases a resource through the UI command flow", async ({ page }) => {
  await page.goto("/");

  await page.locator("#user").selectOption("alice");
  await page.locator("#resource").selectOption(reportResource);
  await page.locator("#ttl").fill("20");

  await page.getByRole("button", { name: "Acquire" }).click();
  await expect(page.locator("#status")).toContainText("acquired: alice");
  await expect(page.locator("#output")).toContainText('"leaseId"');

  await page.getByRole("button", { name: "Release by command" }).click();
  await expect(page.locator("#status")).toContainText("released: alice");
});

test("shows denied write access for a read-only user", async ({ page }) => {
  await page.goto("/");

  await page.locator("#user").selectOption("bob");
  await page.locator("#resource").selectOption(reportResource);

  await page.getByRole("button", { name: "Acquire" }).click();
  await expect(page.locator("#status")).toContainText("denied: bob");
  await expect(page.locator("#output")).toContainText("write access required");
});

test("simulates concurrent access attempts against a shared resource", async ({ page }) => {
  await page.goto("/");

  await page.locator("#resource").selectOption(paymentResource);
  await page.getByRole("button", { name: "Simulate concurrent access" }).click();

  await expect(page.locator("#output")).toContainText('"attempts"');
  await expect(page.locator("#output")).toContainText('"status": "acquired"');
  await expect(page.locator("#output")).toContainText('"status": "denied"');
});
