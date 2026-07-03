package uac.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import uac.ResourceIdentity;
import uac.ResourcePermission;
import uac.UserAccessChecker;
import uac.UserAccessControl;
import uac.UserAccessLevel;
import uac.ValueField;
import uac.WildcardField;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AccessControlDemoServer {

    private static final String REPOSITORY = "demo";
    private static final int DEFAULT_PORT = 8080;

    private final DemoAccessRepository accessRepository = new DemoAccessRepository();
    private final UserAccessChecker accessChecker = new UserAccessChecker(accessRepository);
    private final ResourceLeaseStore leaseStore = new ResourceLeaseStore(accessChecker);

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        new AccessControlDemoServer().start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        leaseStore.start();
        server.start();
        System.out.printf("UserAccessControl demo listening on port %d%n", port);
    }

    private void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(exchange.getRequestMethod()) && "/".equals(path)) {
                respondHtml(exchange, 200, ui());
            } else if ("GET".equals(exchange.getRequestMethod()) && "/api".equals(path)) {
                respond(exchange, 200, routes());
            } else if ("GET".equals(exchange.getRequestMethod()) && "/health".equals(path)) {
                respond(exchange, 200, "{\"status\":\"ok\"}");
            } else if ("GET".equals(exchange.getRequestMethod()) && ("/resources".equals(path) || "/api/resources".equals(path))) {
                respond(exchange, 200, resources());
            } else if ("GET".equals(exchange.getRequestMethod()) && ("/leases".equals(path) || "/api/leases".equals(path))) {
                respond(exchange, 200, leaseStore.leasesJson());
            } else if ("POST".equals(exchange.getRequestMethod()) && ("/acquire".equals(path) || "/api/acquire".equals(path))) {
                respond(exchange, 200, acquire(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && ("/release".equals(path) || "/api/release".equals(path))) {
                respond(exchange, 200, release(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && ("/command".equals(path) || "/api/command".equals(path))) {
                respond(exchange, 200, command(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && ("/simulate".equals(path) || "/api/simulate".equals(path))) {
                respond(exchange, 200, simulate(exchange.getRequestURI()));
            } else {
                respond(exchange, 404, "{\"error\":\"not_found\"}");
            }
        } catch (IllegalArgumentException ex) {
            respond(exchange, 400, "{\"error\":\"" + json(ex.getMessage()) + "\"}");
        } catch (Exception ex) {
            respond(exchange, 500, "{\"error\":\"" + json(ex.getMessage()) + "\"}");
        } finally {
            exchange.close();
        }
    }

    private String acquire(URI uri) {
        Map<String, String> query = query(uri);
        String user = required(query, "user");
        String key = required(query, "key");
        long ttlSeconds = Long.parseLong(query.getOrDefault("ttlSeconds", "30"));
        return leaseStore.acquire(user, key, ttlSeconds).json();
    }

    private String release(URI uri) {
        Map<String, String> query = query(uri);
        String leaseId = query.get("leaseId");
        String key = query.get("key");
        if (leaseId == null && key == null) {
            throw new IllegalArgumentException("leaseId or key is required");
        }
        return leaseStore.release(leaseId, key).json();
    }

    private String command(URI uri) {
        Map<String, String> query = query(uri);
        String command = required(query, "command");
        if (!"release".equals(command)) {
            throw new IllegalArgumentException("supported command: release");
        }
        return release(uri);
    }

    private String simulate(URI uri) {
        Map<String, String> query = query(uri);
        String key = query.getOrDefault("key", "config/payment.yml");
        long ttlSeconds = Long.parseLong(query.getOrDefault("ttlSeconds", "5"));
        String[] users = query.getOrDefault("users", "alice,carol,bob,dave").split(",");

        List<Thread> threads = new ArrayList<>();
        List<String> results = java.util.Collections.synchronizedList(new ArrayList<>());
        for (String rawUser : users) {
            String user = rawUser.trim();
            Thread thread = Thread.ofVirtual().unstarted(() -> {
                LeaseResult result = leaseStore.acquire(user, key, ttlSeconds);
                results.add(result.json());
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("simulation interrupted", ex);
            }
        }

        return "{\"resource\":\"" + AccessControlDemoServer.json(key) + "\",\"attempts\":[" + String.join(",", results) + "]}";
    }

    private String resources() {
        List<String> entries = new ArrayList<>();
        for (String key : accessRepository.resourceKeys()) {
            entries.add("{\"key\":\"" + AccessControlDemoServer.json(key) + "\",\"identity\":" + identityJson(identity(key)) + "}");
        }
        return "{\"repository\":\"" + REPOSITORY + "\",\"resources\":[" + String.join(",", entries) + "]}";
    }

    private static String routes() {
        return """
                {
                  "name": "UserAccessControl demo",
                  "examples": [
                    "GET /resources",
                    "POST /acquire?user=alice&key=config/payment.yml&ttlSeconds=20",
                    "POST /simulate?key=config/payment.yml",
                    "POST /command?command=release&leaseId=<lease-id>",
                    "POST /release?key=config/payment.yml"
                  ],
                  "users": {
                    "alice": "write access to all demo resources",
                    "carol": "write access to config/payment.yml and read access elsewhere",
                    "bob": "read-only access",
                    "dave": "no access"
                  }
                }
                """;
    }

    private static String ui() {
        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>UserAccessControl demo</title>
                    <style>
                      :root {
                        color-scheme: light;
                        font-family: Arial, Helvetica, sans-serif;
                        background: #f6f7f9;
                        color: #1b1f24;
                      }

                      body {
                        margin: 0;
                      }

                      main {
                        max-width: 1040px;
                        margin: 0 auto;
                        padding: 32px 20px;
                      }

                      header {
                        margin-bottom: 28px;
                      }

                      h1 {
                        margin: 0 0 8px;
                        font-size: 32px;
                        letter-spacing: 0;
                      }

                      p {
                        margin: 0;
                        color: #4d5761;
                        line-height: 1.5;
                      }

                      .controls {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                        gap: 16px;
                        margin-bottom: 20px;
                      }

                      label {
                        display: grid;
                        gap: 6px;
                        font-weight: 700;
                        font-size: 14px;
                      }

                      select,
                      input,
                      button {
                        min-height: 42px;
                        border: 1px solid #c7cdd4;
                        border-radius: 6px;
                        padding: 8px 10px;
                        font: inherit;
                        background: #ffffff;
                      }

                      button {
                        border-color: #155eef;
                        background: #155eef;
                        color: #ffffff;
                        font-weight: 700;
                        cursor: pointer;
                      }

                      button.secondary {
                        border-color: #59636e;
                        background: #ffffff;
                        color: #24292f;
                      }

                      button:disabled {
                        cursor: wait;
                        opacity: 0.68;
                      }

                      .actions {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        margin-bottom: 24px;
                      }

                      .status {
                        min-height: 26px;
                        margin-bottom: 18px;
                        font-weight: 700;
                      }

                      .status[data-state="acquired"],
                      .status[data-state="released"] {
                        color: #116329;
                      }

                      .status[data-state="denied"],
                      .status[data-state="locked"],
                      .status[data-state="not_found"] {
                        color: #9a3412;
                      }

                      .grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                        gap: 18px;
                      }

                      section {
                        min-width: 0;
                      }

                      h2 {
                        font-size: 18px;
                        margin: 0 0 10px;
                      }

                      ul {
                        margin: 0;
                        padding-left: 18px;
                        line-height: 1.7;
                      }

                      pre {
                        min-height: 220px;
                        overflow: auto;
                        margin: 0;
                        padding: 14px;
                        border: 1px solid #d0d7de;
                        border-radius: 6px;
                        background: #ffffff;
                        font-size: 13px;
                        line-height: 1.45;
                      }
                    </style>
                  </head>
                  <body>
                    <main>
                      <header>
                        <h1>UserAccessControl demo</h1>
                        <p>Acquire keyed fake repository resources, simulate concurrent access, and release leases by timeout or command.</p>
                      </header>

                      <div class="controls" aria-label="Access controls">
                        <label>
                          User
                          <select id="user">
                            <option value="alice">alice - writer</option>
                            <option value="carol">carol - payment writer</option>
                            <option value="bob">bob - reader</option>
                            <option value="dave">dave - no access</option>
                          </select>
                        </label>
                        <label>
                          Resource
                          <select id="resource"></select>
                        </label>
                        <label>
                          Lease TTL seconds
                          <input id="ttl" type="number" min="1" max="300" value="20">
                        </label>
                      </div>

                      <div class="actions">
                        <button id="acquire">Acquire</button>
                        <button id="release" class="secondary">Release by command</button>
                        <button id="simulate" class="secondary">Simulate concurrent access</button>
                        <button id="refresh" class="secondary">Refresh leases</button>
                      </div>

                      <div id="status" class="status" role="status" aria-live="polite">Loading resources...</div>

                      <div class="grid">
                        <section>
                          <h2>Resources</h2>
                          <ul id="resources"></ul>
                        </section>
                        <section>
                          <h2>Last response</h2>
                          <pre id="output">{}</pre>
                        </section>
                      </div>
                    </main>

                    <script>
                      const state = { leaseId: null };
                      const user = document.querySelector("#user");
                      const resource = document.querySelector("#resource");
                      const ttl = document.querySelector("#ttl");
                      const status = document.querySelector("#status");
                      const output = document.querySelector("#output");
                      const resources = document.querySelector("#resources");
                      const buttons = Array.from(document.querySelectorAll("button"));

                      function setBusy(isBusy) {
                        buttons.forEach((button) => {
                          button.disabled = isBusy;
                        });
                      }

                      function show(message, stateName) {
                        status.textContent = message;
                        status.dataset.state = stateName || "";
                      }

                      function render(data) {
                        output.textContent = JSON.stringify(data, null, 2);
                        if (data.lease && data.lease.leaseId) {
                          state.leaseId = data.lease.leaseId;
                        }
                        if (data.status) {
                          show(`${data.status}: ${data.user || "resource"} ${data.key || ""}`.trim(), data.status);
                        }
                      }

                      async function call(path, options = {}) {
                        setBusy(true);
                        try {
                          const response = await fetch(path, options);
                          const data = await response.json();
                          render(data);
                          return data;
                        } finally {
                          setBusy(false);
                        }
                      }

                      async function loadResources() {
                        const data = await call("/api/resources");
                        resource.innerHTML = "";
                        resources.innerHTML = "";
                        data.resources.forEach((item) => {
                          const option = document.createElement("option");
                          option.value = item.key;
                          option.textContent = item.key;
                          resource.append(option);

                          const row = document.createElement("li");
                          row.textContent = item.key;
                          resources.append(row);
                        });
                        show(`Loaded ${data.resources.length} resources`, "loaded");
                      }

                      document.querySelector("#acquire").addEventListener("click", () => {
                        const params = new URLSearchParams({
                          user: user.value,
                          key: resource.value,
                          ttlSeconds: ttl.value
                        });
                        call(`/api/acquire?${params}`, { method: "POST" });
                      });

                      document.querySelector("#release").addEventListener("click", () => {
                        const params = new URLSearchParams({ command: "release" });
                        if (state.leaseId) {
                          params.set("leaseId", state.leaseId);
                        } else {
                          params.set("key", resource.value);
                        }
                        call(`/api/command?${params}`, { method: "POST" });
                      });

                      document.querySelector("#simulate").addEventListener("click", () => {
                        const params = new URLSearchParams({
                          key: resource.value,
                          ttlSeconds: "5",
                          users: "alice,carol,bob,dave"
                        });
                        call(`/api/simulate?${params}`, { method: "POST" });
                      });

                      document.querySelector("#refresh").addEventListener("click", () => {
                        call("/api/leases");
                      });

                      loadResources().catch((error) => {
                        show(error.message, "error");
                      });
                    </script>
                  </body>
                </html>
                """;
    }

    private static ResourceIdentity identity(String key) {
        return new ResourceIdentity.Builder()
                .field(new ValueField("repository", REPOSITORY))
                .field(new ValueField("key", key))
                .build();
    }

    private static String identityJson(ResourceIdentity identity) {
        List<String> fields = new ArrayList<>();
        identity.getFieldMap().forEach((name, field) ->
                fields.add("\"" + json(name) + "\":\"" + json(field.getValue()) + "\""));
        return "{" + String.join(",", fields) + "}";
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2
                    ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            result.put(key, value);
        }
        return result;
    }

    private static String required(Map<String, String> query, String name) {
        String value = query.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        respond(exchange, status, body, "application/json; charset=utf-8");
    }

    private static void respondHtml(HttpExchange exchange, int status, String body) {
        respond(exchange, status, body, "text/html; charset=utf-8");
    }

    private static void respond(HttpExchange exchange, int status, String body, String contentType) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String json(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static final class DemoAccessRepository implements UserAccessControl {
        private final Map<String, Set<ResourcePermission>> permissions = Map.of(
                "alice", Set.of(permission(new WildcardField("key"), UserAccessLevel.WRITE)),
                "bob", Set.of(permission(new WildcardField("key"), UserAccessLevel.READ)),
                "carol", Set.of(
                        permission(new ValueField("key", "config/payment.yml"), UserAccessLevel.WRITE),
                        permission(new WildcardField("key"), UserAccessLevel.READ)),
                "dave", Set.of(permission(new WildcardField("key"), UserAccessLevel.NONE))
        );

        private final List<String> resourceKeys = List.of(
                "config/payment.yml",
                "secrets/root-token",
                "cache/session-index",
                "reports/monthly.csv"
        );

        @Override
        public Set<ResourcePermission> getPermissionSet(String userName) {
            return permissions.getOrDefault(userName, Set.of());
        }

        List<String> resourceKeys() {
            return resourceKeys;
        }

        private static ResourcePermission permission(uac.IdentityField keyField, UserAccessLevel level) {
            ResourceIdentity identity = new ResourceIdentity.Builder()
                    .field(new ValueField("repository", REPOSITORY))
                    .field(keyField)
                    .build();
            return new ResourcePermission(identity, level);
        }
    }

    private static final class ResourceLeaseStore {
        private final UserAccessChecker accessChecker;
        private final Map<String, ResourceLease> leasesById = new ConcurrentHashMap<>();
        private final Map<String, ResourceLease> leasesByKey = new ConcurrentHashMap<>();
        private final ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor();

        ResourceLeaseStore(UserAccessChecker accessChecker) {
            this.accessChecker = accessChecker;
        }

        void start() {
            janitor.scheduleAtFixedRate(this::releaseExpired, 1, 1, TimeUnit.SECONDS);
        }

        synchronized LeaseResult acquire(String user, String key, long ttlSeconds) {
            if (ttlSeconds < 1 || ttlSeconds > 300) {
                throw new IllegalArgumentException("ttlSeconds must be between 1 and 300");
            }
            UserAccessLevel accessLevel = accessChecker.getLevel(user, identity(key));
            if (accessLevel != UserAccessLevel.WRITE) {
                return LeaseResult.denied(user, key, accessLevel);
            }

            ResourceLease current = leasesByKey.get(key);
            if (current != null && current.expiresAt().isAfter(Instant.now())) {
                return LeaseResult.locked(user, key, current);
            }

            ResourceLease lease = new ResourceLease(
                    UUID.randomUUID().toString(),
                    user,
                    key,
                    Instant.now().plusSeconds(ttlSeconds));
            leasesById.put(lease.id(), lease);
            leasesByKey.put(key, lease);
            return LeaseResult.acquired(lease);
        }

        synchronized LeaseResult release(String leaseId, String key) {
            ResourceLease lease = leaseId != null ? leasesById.remove(leaseId) : leasesByKey.get(key);
            if (lease == null) {
                return LeaseResult.notFound(leaseId, key);
            }
            leasesById.remove(lease.id());
            leasesByKey.remove(lease.key());
            return LeaseResult.released(lease);
        }

        String leasesJson() {
            List<String> entries = leasesById.values().stream()
                    .sorted(java.util.Comparator.comparing(ResourceLease::key))
                    .map(ResourceLease::json)
                    .toList();
            return "{\"leases\":[" + String.join(",", entries) + "]}";
        }

        private synchronized void releaseExpired() {
            Instant now = Instant.now();
            for (ResourceLease lease : List.copyOf(leasesById.values())) {
                if (!lease.expiresAt().isAfter(now)) {
                    leasesById.remove(lease.id());
                    leasesByKey.remove(lease.key());
                }
            }
        }
    }

    private record ResourceLease(String id, String user, String key, Instant expiresAt) {
        String json() {
            return "{\"leaseId\":\"" + AccessControlDemoServer.json(id) + "\",\"user\":\"" + AccessControlDemoServer.json(user) + "\",\"key\":\""
                    + AccessControlDemoServer.json(key) + "\",\"expiresAt\":\"" + expiresAt + "\"}";
        }
    }

    private record LeaseResult(String status, String user, String key, UserAccessLevel accessLevel,
                               ResourceLease lease, String message) {
        static LeaseResult acquired(ResourceLease lease) {
            return new LeaseResult("acquired", lease.user(), lease.key(), UserAccessLevel.WRITE, lease, null);
        }

        static LeaseResult released(ResourceLease lease) {
            return new LeaseResult("released", lease.user(), lease.key(), UserAccessLevel.WRITE, lease, null);
        }

        static LeaseResult denied(String user, String key, UserAccessLevel accessLevel) {
            return new LeaseResult("denied", user, key, accessLevel, null, "write access required");
        }

        static LeaseResult locked(String user, String key, ResourceLease lease) {
            return new LeaseResult("locked", user, key, UserAccessLevel.WRITE, lease, "resource already leased");
        }

        static LeaseResult notFound(String leaseId, String key) {
            String target = leaseId != null ? leaseId : key;
            return new LeaseResult("not_found", null, key, UserAccessLevel.NONE, null,
                    "lease not found: " + target);
        }

        String json() {
            List<String> fields = new ArrayList<>();
            fields.add("\"status\":\"" + AccessControlDemoServer.json(status) + "\"");
            if (user != null) fields.add("\"user\":\"" + AccessControlDemoServer.json(user) + "\"");
            if (key != null) fields.add("\"key\":\"" + AccessControlDemoServer.json(key) + "\"");
            fields.add("\"accessLevel\":\"" + accessLevel + "\"");
            if (message != null) fields.add("\"message\":\"" + AccessControlDemoServer.json(message) + "\"");
            if (lease != null) fields.add("\"lease\":" + lease.json());
            return "{" + String.join(",", fields) + "}";
        }
    }
}
