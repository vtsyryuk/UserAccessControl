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
                respond(exchange, 200, routes());
            } else if ("GET".equals(exchange.getRequestMethod()) && "/health".equals(path)) {
                respond(exchange, 200, "{\"status\":\"ok\"}");
            } else if ("GET".equals(exchange.getRequestMethod()) && "/resources".equals(path)) {
                respond(exchange, 200, resources());
            } else if ("GET".equals(exchange.getRequestMethod()) && "/leases".equals(path)) {
                respond(exchange, 200, leaseStore.leasesJson());
            } else if ("POST".equals(exchange.getRequestMethod()) && "/acquire".equals(path)) {
                respond(exchange, 200, acquire(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && "/release".equals(path)) {
                respond(exchange, 200, release(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && "/command".equals(path)) {
                respond(exchange, 200, command(exchange.getRequestURI()));
            } else if ("POST".equals(exchange.getRequestMethod()) && "/simulate".equals(path)) {
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
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
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
                "alice", Set.of(permission(new WildcardField("key"), UserAccessLevel.Write)),
                "bob", Set.of(permission(new WildcardField("key"), UserAccessLevel.Read)),
                "carol", Set.of(
                        permission(new ValueField("key", "config/payment.yml"), UserAccessLevel.Write),
                        permission(new WildcardField("key"), UserAccessLevel.Read)),
                "dave", Set.of(permission(new WildcardField("key"), UserAccessLevel.None))
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
            if (accessLevel != UserAccessLevel.Write) {
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
            return new LeaseResult("acquired", lease.user(), lease.key(), UserAccessLevel.Write, lease, null);
        }

        static LeaseResult released(ResourceLease lease) {
            return new LeaseResult("released", lease.user(), lease.key(), UserAccessLevel.Write, lease, null);
        }

        static LeaseResult denied(String user, String key, UserAccessLevel accessLevel) {
            return new LeaseResult("denied", user, key, accessLevel, null, "write access required");
        }

        static LeaseResult locked(String user, String key, ResourceLease lease) {
            return new LeaseResult("locked", user, key, UserAccessLevel.Write, lease, "resource already leased");
        }

        static LeaseResult notFound(String leaseId, String key) {
            String target = leaseId != null ? leaseId : key;
            return new LeaseResult("not_found", null, key, UserAccessLevel.None, null,
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
