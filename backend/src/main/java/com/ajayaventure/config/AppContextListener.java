package com.ajayaventure.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application lifecycle hook. Logs build/version information and validates that
 * required configuration is present. Startup intentionally does not require a
 * live database so that container readiness probes can report accurately.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    public static final String APP_NAME = "ajaya-venture";
    public static final String APP_VERSION = "1.0.0-SNAPSHOT";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String env = com.ajayaventure.util.EnvVars.get("APP_ENV", "development");
        log.info("{} v{} starting | env={}", APP_NAME, APP_VERSION, env);
        sce.getServletContext().setAttribute("av.app.version", APP_VERSION);
        sce.getServletContext().setAttribute("av.app.env", env);

        String dbUser = com.ajayaventure.util.EnvVars.get("DB_USERNAME", "AV_APP");
        String dbService = com.ajayaventure.util.EnvVars.get("DB_SERVICE", "orclpdb");
        if ("AV_APP".equals(dbUser) && "change-me".equals(
                com.ajayaventure.util.EnvVars.get("DB_PASSWORD", "change-me"))) {
            log.warn("Default DB credentials detected. Configure .env before production use.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("{} stopping", APP_NAME);
        Database.reset();
    }
}