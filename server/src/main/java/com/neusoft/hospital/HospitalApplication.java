package com.neusoft.hospital;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class HospitalApplication {

    /**
     * Load `server/.env` into System env vars BEFORE Spring starts, so that
     * placeholders like `${DASHSCOPE_API_KEY}` resolve correctly.
     *
     * <p>The .env file is .gitignore'd; we never log its contents. Existing
     * real env vars take precedence (so production deploys can override the
     * dev .env).</p>
     */
    public static void main(String[] args) {
        loadDotenvIfPresent();
        SpringApplication.run(HospitalApplication.class, args);
    }

    private static void loadDotenvIfPresent() {
        try {
            Dotenv.configure()
                    .ignoreIfMissing()
                    .systemProperties()
                    .load()
                    .entries()
                    .forEach(e -> {
                        String key = e.getKey();
                        String value = e.getValue();
                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value);
                        }
                    });
        } catch (Exception ignored) {
            // dotenv-java not on classpath or .env unreadable → silently fall
            // back to real environment variables. Required vars will then
            // surface a clear "missing" error during startup.
        }
    }
}