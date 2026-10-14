package com.campus.lostfound.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

/**
 * Makes files saved in the ./uploads folder visible in the browser at
 * the address /uploads/<filename> — without this, the photo files would
 * exist on disk but there'd be no URL to actually view them.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = new File("uploads").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + File.separator);
    }
}
