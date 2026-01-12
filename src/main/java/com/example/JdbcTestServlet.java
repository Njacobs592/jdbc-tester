package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@WebServlet("/*")
public class JdbcTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        /* --------------------
           LOAD DB CONFIGS
           -------------------- */
        List<DbConfig> dbs = loadDbConfigs();

        /* ---- HARD GUARD ---- */
        if (dbs.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().println("ERROR: No database configurations loaded");
            resp.getWriter().println("Check DB_CONFIG_PATH or databases.properties");
            resp.getWriter().flush();
            return;
        }

        /* --------------------
           RUN TESTS
           -------------------- */
        List<DbResult> results = new ArrayList<>();
        boolean allHealthy = true;

        for (DbConfig db : dbs) {
            DbResult r = testConnection(db);
            results.add(r);
            if (!r.success) {
                allHealthy = false;
            }
        }

        /* --------------------
           /health
           -------------------- */
        if (path.endsWith("/health")) {
            resp.setContentType("text/plain");

            if (allHealthy) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("OK");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().println("UNHEALTHY");
                for (DbResult r : results) {
                    if (!r.success) {
                        resp.getWriter().println(
                            r.name + ": " + r.message
                        );
                    }
                }
            }
            resp.getWriter().flush();
            return;
        }

        /* --------------------
           /download
           -------------------- */
        if (path.endsWith("/download")) {
            resp.setContentType("application/x-yaml");
            resp.setHeader(
                "Content-Disposition",
                "attachment; filename=\"db_results.yaml\""
            );

            List<Map<String,Object>> yamlList = new ArrayList<>();
            for (DbResult r : results) {
                yamlList.add(r.toMap());
            }

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(yamlList, resp.getWriter());
            resp.getWriter().flush();
            return;
        }

        /* --------------------
           DEFAULT OUTPUT
           -------------------- */
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        for (DbResult r : results) {
            out.println(
                r.name + ": " +
                (r.success ? "SUCCESS" : "FAIL: " + r.message)
            );
        }
        out.flush();
    }

    /* =========================================================
       CONFIG LOADER
       ========================================================= */
    private List<DbConfig> loadDbConfigs() throws IOException {
        System.out.println("ENV DB_CONFIG_PATH=" + System.getenv("DB_CONFIG_PATH"));
        List<DbConfig> list = new ArrayList<>();
        Properties props = new Properties();

        String externalPath = System.getenv("DB_CONFIG_PATH");

        if (externalPath == null || externalPath.trim().isEmpty()) {
            throw new IllegalStateException("DB_CONFIG_PATH must be set");
        }

        System.out.println("Loading DB config from " + externalPath);

        try (InputStream in = new FileInputStream(externalPath)) {
            props.load(in);
        }

        for (int i = 1; ; i++) {
            String prefix = "db" + i;
            String name = props.getProperty(prefix + ".name");
            if (name == null) {
                break;
            }

            DbConfig db = new DbConfig();
            db.name = name;
            db.driver = props.getProperty(prefix + ".driver");
            db.url = props.getProperty(prefix + ".url");
            db.user = props.getProperty(prefix + ".user");
            db.password = props.getProperty(prefix + ".password");

            list.add(db);
        }

        return list;
    }

    /* =========================================================
       CONNECTION TEST
       ========================================================= */
    private DbResult testConnection(DbConfig db) {
        DbResult r = new DbResult();
        r.name = db.name;

        try {
            Class.forName(db.driver);
            try (Connection c = DriverManager.getConnection(
                    db.url, db.user, db.password)) {
                r.success = true;
            }
        } catch (Exception e) {
            r.success = false;
            r.message = e.getMessage();
        }

        return r;
    }

    /* =========================================================
       DATA HOLDERS
       ========================================================= */
    static class DbConfig {
        String name;
        String driver;
        String url;
        String user;
        String password;
    }

    static class DbResult {
        String name;
        boolean success;
        String message;

        Map<String,Object> toMap() {
            Map<String,Object> m = new HashMap<>();
            m.put("name", name);
            m.put("success", success);
            if (!success) {
                m.put("error", message);
            }
            return m;
        }
    }
}