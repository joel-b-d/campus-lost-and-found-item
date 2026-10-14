package com.campus.lostfound.config;

import com.campus.lostfound.service.ConnectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs once, right after the app has started. It prints the addresses to
 * use (IntelliJ turns them into clickable links) and opens the login page
 * in this laptop's browser, so you never have to type anything.
 *
 * To turn the auto-open off, add this line to application.properties:
 *     app.open-browser=false
 */
@Component
public class StartupBanner {

    @Autowired
    private ConnectService connectService;

    @Value("${app.open-browser:true}")
    private boolean openBrowser;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        int port = connectService.getPort();
        String local = "http://localhost:" + port;
        List<ConnectService.Entry> entries = connectService.findEntries();

        System.out.println();
        System.out.println("==================================================================");
        System.out.println("  Campus Lost & Found is running");
        System.out.println("  On this laptop :  " + local + "/login");
        for (ConnectService.Entry e : entries) {
            System.out.println("  From other devices (" + e.getLabel() + "):");
            System.out.println("                    " + e.getUrl());
        }
        System.out.println("  Easiest for phones: open " + local + "/connect and scan the QR code");
        System.out.println("  (devices must be on the SAME network as this laptop)");
        System.out.println("==================================================================");
        System.out.println();

        if (openBrowser) {
            openInBrowser(local + "/login");
        }
    }

    // Windows only; on any other system or on failure we just skip it.
    private void openInBrowser(String url) {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Exception ignored) {
            // Not important — the address is printed above anyway.
        }
    }
}
