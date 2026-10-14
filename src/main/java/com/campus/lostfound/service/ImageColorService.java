package com.campus.lostfound.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compares two uploaded photos by their COLOURS.
 *
 * How it works, in plain words:
 *   1. Open the photo and look only at its centre (the middle 60%), because
 *      the item is usually in the middle and the edges are mostly background.
 *   2. Sample a grid of 48 x 48 pixels from that centre.
 *   3. Sort every sampled pixel into one of 15 colour "buckets":
 *        black, white, grey, and 12 hues (red, orange, yellow, green, ...).
 *   4. Count how much of the photo falls in each bucket. This list of 15
 *      percentages is the photo's "colour histogram".
 *   5. Compare two histograms with "histogram intersection": for each bucket,
 *      take the smaller of the two percentages, then add them up.
 *        1.0 = identical colour mix, 0.0 = nothing in common.
 *
 * This is only a hint — lighting and background change colours — so
 * MatchingService gives it a small weight next to the text score.
 *
 * Everything here is fail-safe: a missing, unreadable or non-image file just
 * means "no photo score", and matching carries on using the text alone.
 */
@Service
public class ImageColorService {

    private static final int BUCKETS = 15;   // black, white, grey + 12 hues
    private static final int GRID = 48;      // we sample 48 x 48 pixels, not every pixel (fast)
    private static final double[] UNREADABLE = new double[0]; // marker: "couldn't read this file"

    // Uploaded files never change (each name has a random prefix), so we can
    // remember each photo's histogram and avoid re-reading it on every page load.
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    /**
     * @return 0.0 to 1.0, or null if either item has no photo or a photo can't be read.
     */
    public Double similarity(String fileNameA, String fileNameB) {
        if (fileNameA == null || fileNameB == null) return null;

        double[] a = histogramFor(fileNameA);
        double[] b = histogramFor(fileNameB);
        if (a.length == 0 || b.length == 0) return null;

        double overlap = 0;
        for (int i = 0; i < BUCKETS; i++) {
            overlap += Math.min(a[i], b[i]); // histogram intersection
        }
        return overlap;
    }

    private double[] histogramFor(String fileName) {
        double[] cached = cache.get(fileName);
        if (cached != null) return cached;

        double[] computed = computeHistogram(fileName);
        if (computed.length > 0) cache.put(fileName, computed); // only remember successes
        return computed;
    }

    private double[] computeHistogram(String fileName) {
        try {
            // Safety: only read files that really live inside the uploads folder.
            Path folder = Path.of("uploads").toAbsolutePath().normalize();
            Path file = folder.resolve(fileName).normalize();
            if (!file.startsWith(folder) || !Files.isRegularFile(file)) return UNREADABLE;

            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) return UNREADABLE; // not an image Java understands

            int w = image.getWidth();
            int h = image.getHeight();

            // The centre crop: skip 20% on each side.
            int cropX = (int) (w * 0.2);
            int cropY = (int) (h * 0.2);
            int cropW = Math.max(1, (int) (w * 0.6));
            int cropH = Math.max(1, (int) (h * 0.6));

            double[] histogram = new double[BUCKETS];
            int samples = 0;

            for (int gy = 0; gy < GRID; gy++) {
                for (int gx = 0; gx < GRID; gx++) {
                    int x = cropX + gx * cropW / GRID;
                    int y = cropY + gy * cropH / GRID;
                    int rgb = image.getRGB(x, y);

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    histogram[bucketFor(r, g, b)]++;
                    samples++;
                }
            }

            // Turn counts into fractions that add up to 1.0
            for (int i = 0; i < BUCKETS; i++) histogram[i] /= samples;
            return histogram;

        } catch (Exception e) {
            return UNREADABLE; // never let a bad photo break the matching page
        }
    }

    /**
     * Which of the 15 colour buckets does this pixel belong to?
     * We use HSB (hue, saturation, brightness) because it separates "which
     * colour" from "how light/dark", which is how people describe items.
     */
    private int bucketFor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null); // [hue 0-1, saturation 0-1, brightness 0-1]
        float hue = hsb[0];
        float saturation = hsb[1];
        float brightness = hsb[2];

        if (brightness < 0.20f) return 0;                       // very dark  -> black
        if (saturation < 0.15f) {                               // barely any colour
            return brightness > 0.80f ? 1 : 2;                  //   bright -> white, otherwise grey
        }
        return 3 + Math.min(11, (int) (hue * 12));              // 12 hues, buckets 3..14
    }
}
