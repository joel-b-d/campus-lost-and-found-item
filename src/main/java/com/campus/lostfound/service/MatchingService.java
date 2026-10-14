package com.campus.lostfound.service;

import com.campus.lostfound.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Suggests which FOUND items are most likely to be the same object as a
 * given LOST item. Staff still make the final decision — this just sorts
 * the list so the best guesses are at the top.
 *
 * The algorithm is TF-IDF + cosine similarity, a classic text-matching
 * method (the same idea early search engines used). In plain words:
 *
 *   1. Turn every item's text (title, description, location) into a list
 *      of lowercase words, dropping filler words like "the" and "my".
 *   2. TF  (term frequency):   a word that appears more often in an item
 *                              matters more to that item.
 *      IDF (inverse doc freq): a word that appears in EVERY item (like
 *                              "black") says little; a rare word (like
 *                              "airpods") says a lot. Rare words get a
 *                              higher weight.
 *   3. Each item becomes a list of numbers (one weight per word) — a
 *      "vector". Two items that use the same important words point in
 *      nearly the same direction.
 *   4. Cosine similarity measures the angle between two vectors:
 *      1.0 = identical wording, 0.0 = nothing in common.
 *
 * On top of the text score, if BOTH items have a photo, a small bonus comes
 * from how similar the photos' colours are (see ImageColorService).
 *
 * No library needed — it's all plain Java.
 */
@Service
public class MatchingService {

    @Autowired
    private ImageColorService imageColorService;

    // How much each signal counts when BOTH items have a photo.
    // Text is the main signal; photo colour is a small extra hint.
    // If either item has no photo, the text score is used on its own (no penalty).
    private static final double TEXT_WEIGHT = 0.85;
    private static final double IMAGE_WEIGHT = 0.15;

    // Filler words that say nothing about WHICH item it is.
    // "lost" and "found" are here because every report contains them.
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "in", "on", "at", "to", "is", "it", "its",
            "my", "me", "i", "we", "with", "for", "near", "was", "were", "by", "from", "this",
            "that", "has", "have", "had", "lost", "found", "left", "some", "very", "there",
            "someone", "please", "around", "inside", "outside");

    // ---------------- Result classes used by the matching page ----------------

    /** One found item plus how well it matches, as percentages (0-100). */
    public static class Candidate {
        private final Item item;
        private final int percent;          // the final score used for ranking
        private final int textPercent;       // from the words (TF-IDF + cosine)
        private final Integer imagePercent;  // from photo colours; null if a photo is missing

        public Candidate(Item item, int percent, int textPercent, Integer imagePercent) {
            this.item = item;
            this.percent = percent;
            this.textPercent = textPercent;
            this.imagePercent = imagePercent;
        }

        public Item getItem() { return item; }
        public int getPercent() { return percent; }
        public int getTextPercent() { return textPercent; }
        public Integer getImagePercent() { return imagePercent; }
    }

    /** One lost item plus its found-item candidates, best match first. */
    public static class Suggestion {
        private final Item lostItem;
        private final List<Candidate> candidates;

        public Suggestion(Item lostItem, List<Candidate> candidates) {
            this.lostItem = lostItem;
            this.candidates = candidates;
        }

        public Item getLostItem() { return lostItem; }
        public List<Candidate> getCandidates() { return candidates; }
    }

    // ---------------- The public method the controller calls ----------------

    public List<Suggestion> buildSuggestions(List<Item> lostItems, List<Item> foundItems) {

        // Step 1: tokenize every item once.
        Map<Item, List<String>> tokens = new HashMap<>();
        List<Item> corpus = new ArrayList<>();
        corpus.addAll(lostItems);
        corpus.addAll(foundItems);
        for (Item item : corpus) {
            tokens.put(item, tokenize(item));
        }

        // Step 2: IDF — in how many items does each word appear?
        Map<String, Integer> docFrequency = new HashMap<>();
        for (Item item : corpus) {
            for (String word : new HashSet<>(tokens.get(item))) {
                docFrequency.merge(word, 1, Integer::sum);
            }
        }
        int totalDocs = corpus.size();
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : docFrequency.entrySet()) {
            // "Smoothed" IDF so the weight is never zero or negative.
            idf.put(e.getKey(), Math.log((totalDocs + 1.0) / (e.getValue() + 1.0)) + 1.0);
        }

        // Step 3: turn each item into a TF-IDF vector.
        Map<Item, Map<String, Double>> vectors = new HashMap<>();
        for (Item item : corpus) {
            vectors.put(item, toVector(tokens.get(item), idf));
        }

        // Step 4: compare each lost item against every found item.
        List<Suggestion> result = new ArrayList<>();
        for (Item lost : lostItems) {
            List<Candidate> candidates = new ArrayList<>();
            for (Item found : foundItems) {
                double textScore = cosine(vectors.get(lost), vectors.get(found));

                // Photo colour similarity — null when either item has no readable photo.
                Double imageScore = imageColorService.similarity(
                        lost.getImageFileName(), found.getImageFileName());

                double finalScore = (imageScore == null)
                        ? textScore
                        : TEXT_WEIGHT * textScore + IMAGE_WEIGHT * imageScore;

                candidates.add(new Candidate(
                        found,
                        (int) Math.round(finalScore * 100),
                        (int) Math.round(textScore * 100),
                        imageScore == null ? null : (int) Math.round(imageScore * 100)));
            }
            candidates.sort(Comparator.comparingInt(Candidate::getPercent).reversed());
            result.add(new Suggestion(lost, candidates));
        }
        return result;
    }

    // ---------------- Helpers ----------------

    /** Splits an item's text into clean lowercase words. */
    private List<String> tokenize(Item item) {
        // The title is repeated 3 times so it counts for more than the
        // description — "black wallet" in the title is the strongest clue.
        String title = nullToEmpty(item.getTitle());
        String text = title + " " + title + " " + title + " "
                + nullToEmpty(item.getDescription()) + " "
                + nullToEmpty(item.getLocation());

        List<String> words = new ArrayList<>();
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() < 2 || STOP_WORDS.contains(raw)) continue;
            words.add(simpleStem(raw));
        }
        return words;
    }

    /** Very light "stemming" so "keys" matches "key" and "bottles" matches "bottle". */
    private String simpleStem(String word) {
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /** word -> (how often it appears) x (how rare it is across all items) */
    private Map<String, Double> toVector(List<String> words, Map<String, Double> idf) {
        Map<String, Double> vector = new HashMap<>();
        for (String word : words) {
            vector.merge(word, 1.0, Double::sum); // this is the TF part (a count)
        }
        for (Map.Entry<String, Double> e : vector.entrySet()) {
            e.setValue(e.getValue() * idf.getOrDefault(e.getKey(), 1.0)); // times IDF
        }
        return vector;
    }

    /** Cosine similarity: dot(a, b) / (length(a) * length(b)). Result is 0.0 to 1.0. */
    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other != null) dot += e.getValue() * other;
        }
        double lengthA = length(a);
        double lengthB = length(b);
        if (lengthA == 0 || lengthB == 0) return 0;
        return dot / (lengthA * lengthB);
    }

    private double length(Map<String, Double> vector) {
        double sum = 0;
        for (double v : vector.values()) sum += v * v;
        return Math.sqrt(sum);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
