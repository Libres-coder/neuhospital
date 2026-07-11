package com.neusoft.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves the local upload directory so that pre-signed URLs handed back to
 * Qwen-VL (or shown to the user as a thumbnail) actually resolve.
 *
 * <p>For production, replace this with an OSS / S3 / CDN front. The Bailian
 * model just needs an HTTP-reachable URL, so a public bucket works without
 * any code change.</p>
 */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path abs = Paths.get(uploadDir).toAbsolutePath();
        File f = abs.toFile();
        if (!f.exists()) f.mkdirs();
        // file:...  form is OS-portable (Windows paths work without extra escaping)
        String location = abs.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
