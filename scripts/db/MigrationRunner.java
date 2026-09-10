import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Applies SQL migration files to the database without Flyway.
 *
 * <p>This is a stopgap for the shared Supabase instance, whose {@code public} schema was created
 * outside Flyway and therefore has no schema history table. It applies the same files that live in
 * {@code src/main/resources/db/migration}, so there is a single source of truth: when Flyway is
 * re-enabled with a proper baseline, these files remain authoritative and nothing has to be
 * rewritten.
 *
 * <p>Applied files are recorded in {@code manual_migration_history}, which makes re-runs safe.
 *
 * <p>Configuration comes from the environment: {@code JDBC_URL}, {@code DB_USER},
 * {@code DB_PASSWORD}. The wrapper script reads them from {@code .env}.
 *
 * <p>Usage: {@code java -cp <postgres-driver.jar> MigrationRunner.java <dir> <regex> [--dry-run]}
 */
public class MigrationRunner {

    private static final String HISTORY_TABLE = "manual_migration_history";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MigrationRunner <migrations-dir> <filename-regex> [--dry-run]");
            System.exit(2);
        }

        Path directory = Path.of(args[0]);
        Pattern pattern = Pattern.compile(args[1]);
        boolean dryRun = args.length > 2 && "--dry-run".equals(args[2]);

        String url = required("JDBC_URL");
        String user = required("DB_USER");
        String password = required("DB_PASSWORD");

        List<Path> candidates = findMigrations(directory, pattern);
        if (candidates.isEmpty()) {
            System.out.println("No migration files matching /" + pattern + "/ in " + directory);
            return;
        }

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            ensureHistoryTable(connection, dryRun);
            Set<String> applied = loadApplied(connection);

            int executed = 0;
            for (Path migration : candidates) {
                String name = migration.getFileName().toString();
                if (applied.contains(name)) {
                    System.out.println("SKIP    " + name + "  (already applied)");
                    continue;
                }
                if (dryRun) {
                    System.out.println("WOULD   " + name);
                    continue;
                }
                apply(connection, migration, name);
                System.out.println("APPLIED " + name);
                executed++;
            }

            if (dryRun) {
                connection.rollback();
                System.out.println("\nDry run — nothing was written.");
            } else {
                connection.commit();
                System.out.println("\nDone. " + executed + " migration(s) applied.");
            }
        }
    }

    private static List<Path> findMigrations(Path directory, Pattern pattern) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> matches = new ArrayList<>(files
                    .filter(Files::isRegularFile)
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .toList());
            matches.sort(Comparator.comparing(path -> path.getFileName().toString()));
            return matches;
        }
    }

    private static void ensureHistoryTable(Connection connection, boolean dryRun) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE + " ("
                    + "filename    VARCHAR(255) PRIMARY KEY,"
                    + "applied_at  TIMESTAMPTZ NOT NULL DEFAULT NOW())");
        }
        if (!dryRun) {
            connection.commit();
        }
    }

    private static Set<String> loadApplied(Connection connection) throws SQLException {
        Set<String> applied = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT filename FROM " + HISTORY_TABLE)) {
            while (rs.next()) {
                applied.add(rs.getString(1));
            }
        }
        return applied;
    }

    private static void apply(Connection connection, Path migration, String name) throws Exception {
        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + HISTORY_TABLE + " (filename) VALUES (?)")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            System.err.println("Missing required environment variable: " + key);
            System.exit(2);
        }
        return value;
    }
}
