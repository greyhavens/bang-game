//
// $Id$

package com.threerings.bang.server;

import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.log;

/**
 * Applies versioned Flyway schema migrations at server startup (Epic 3 Phase 7).
 *
 * <p><b>Policy — baseline + freeze:</b> the pre-existing schema (built by two decades of Depot
 * auto-migration plus the legacy repositories' constructor-time DDL) is baselined as version 1;
 * every schema change FROM NOW ON ships as a versioned SQL file under
 * {@code server/src/main/resources/db/migration/<database>/} (see the runbook's "Schema
 * migrations" section). The legacy auto-create paths stay untouched as the fresh-install
 * bootstrap — neutralizing them across 8+ repositories is deliberately out of scope.
 *
 * <p>Must run before ANY persistence code: the legacy repositories run their DDL inside their
 * constructors during Guice module setup, so this is invoked from {@code BangServer.main} before
 * the injector is created. A failed or half-applied migration aborts startup (fail closed) —
 * booting the game against a partially-migrated schema corrupts data in ways a crash doesn't.
 */
public class BangSchemaMigrations
{
    /**
     * Applies pending migrations to the game ({@code bang}) and user ({@code ooouser}) databases.
     * Exits the process on failure.
     */
    public static void apply ()
    {
        Properties db = ServerConfig.getJDBCConfig();
        // the user DB may live on a separate host with its own credentials (db.userdb.* overrides
        // in server.properties); each falls back to the db.default.* values
        migrate("bang", db.getProperty("default.url"),
                db.getProperty("default.username"), db.getProperty("default.password"));
        migrate("ooouser", value(db, "userdb.url", "default.url"),
                value(db, "userdb.username", "default.username"),
                value(db, "userdb.password", "default.password"));
    }

    protected static void migrate (String database, String url, String username, String password)
    {
        if (StringUtil.isBlank(url)) {
            log.warning("No JDBC url configured; skipping schema migrations", "db", database);
            return;
        }
        try {
            MigrateResult result = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration/" + database)
                // existing installs predate Flyway: on first contact, record the current schema
                // as version 1 (the V1 baseline marker) instead of trying to recreate it
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();
            if (result.migrationsExecuted > 0) {
                log.info("Applied schema migrations", "db", database,
                         "count", result.migrationsExecuted, "version", result.targetSchemaVersion);
            }
        } catch (Exception e) {
            log.warning("Schema migration FAILED; refusing to start against a possibly " +
                        "half-migrated database.", "db", database, e);
            System.exit(255);
        }
    }

    /** Returns the {@code primary} property, falling back to {@code fallback}. */
    protected static String value (Properties props, String primary, String fallback)
    {
        String value = props.getProperty(primary);
        return StringUtil.isBlank(value) ? props.getProperty(fallback) : value;
    }
}
