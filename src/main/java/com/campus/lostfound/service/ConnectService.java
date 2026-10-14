package com.campus.lostfound.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Works out the web addresses other devices can use to reach this app, and
 * draws a QR code for each so a phone can just scan it.
 *
 * Why this exists: this laptop can have several network addresses at once
 * (Wi-Fi, Ethernet, hotspot, virtual adapters). People waste time guessing
 * which one to type. Here the app lists them all for you.
 */
@Service
public class ConnectService {

    @Value("${server.port:8080}")
    private int port;

    /** One reachable address: a label ("Wi-Fi"), the full URL, and its QR code as SVG. */
    public static class Entry {
        private final String label;
        private final String url;
        private final String qrSvg;

        public Entry(String label, String url, String qrSvg) {
            this.label = label;
            this.url = url;
            this.qrSvg = qrSvg;
        }

        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public String getQrSvg() { return qrSvg; }
    }

    public int getPort() {
        return port;
    }

    /** Every usable address of this laptop, as ready-to-open login URLs. */
    public List<Entry> findEntries() {
        List<Entry> entries = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;

                // Skip adapters that other devices cannot reach (VirtualBox, VMware,
                // Hyper-V, Docker, WSL, Bluetooth). We match specific names on purpose.
                // We do NOT skip every name containing "virtual", because Windows calls
                // its Mobile Hotspot adapter "Wi-Fi Direct Virtual Adapter" and that
                // one must stay in the list.
                String name = nic.getDisplayName() == null ? "" : nic.getDisplayName().toLowerCase();
                if (isUnreachableAdapter(name)) continue;

                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (!(address instanceof Inet4Address)) continue;   // IPv4 only (simpler to type)
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) continue;

                    String url = "http://" + address.getHostAddress() + ":" + port + "/login";
                    entries.add(new Entry(nic.getDisplayName(), url, qrSvg(url)));
                }
            }
        } catch (Exception e) {
            // If the network can't be read, we simply show an empty list.
        }
        return entries;
    }

    /** True for adapters that only exist inside this laptop (virtual machines, containers, Bluetooth). */
    private boolean isUnreachableAdapter(String name) {
        return name.contains("virtualbox") || name.contains("host-only")
                || name.contains("vmware") || name.contains("vethernet")
                || name.contains("hyper-v") || name.contains("docker")
                || name.contains("wsl") || name.contains("bluetooth")
                || name.contains("pseudo");
    }

    /**
     * Draws a QR code for the text as an inline SVG image.
     * ZXing gives us a grid of black/white squares; we turn each black square
     * into a tiny SVG rectangle. No image files, no extra libraries.
     */
    public String qrSvg(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0);
            int quiet = 4;                          // white border QR readers need
            int size = matrix.getWidth() + quiet * 2;

            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ")
               .append(size).append(' ').append(size)
               .append("' shape-rendering='crispEdges'>")
               .append("<rect width='100%' height='100%' fill='white'/><path fill='black' d='");
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        svg.append('M').append(x + quiet).append(' ').append(y + quiet).append("h1v1h-1z");
                    }
                }
            }
            svg.append("'/></svg>");
            return svg.toString();
        } catch (WriterException e) {
            return "";
        }
    }
}
