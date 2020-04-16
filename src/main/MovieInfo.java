package main;

import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashSet;

public class MovieInfo {
    String title, year, plot, posterURL, absolutePath, subtitleAbsolutePath, imdbRating, director;
    HashSet<String> genres;
    HashSet<String> tags;

    VBox view;
    MovieInfo(String t, String y, String g, String p, String pu, String a, String s, String i, String d) {
        this.year = y;
        this.title = t;
        this.genres = new HashSet<>(Arrays.asList(g.split(", ")));
        this.plot = p;
        this.posterURL = pu;
        this.absolutePath = a;
        this.subtitleAbsolutePath = s;
        this.imdbRating = i;
        this.director = d;
        this.view = null;

        this.tags = new HashSet<>();
        tags.addAll(Arrays.asList(t.toLowerCase().split(" ")));
        tags.addAll(Arrays.asList(d.toLowerCase().split(" ")));
        tags.add(y);
    }

    void setView(VBox v) {
        this.view = v;
    }
}