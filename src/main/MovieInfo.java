package main;

import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashSet;

public class MovieInfo {
    String title, plot, posterURL, absolutePath, subtitleAbsolutePath;
    HashSet<String> genres;
    int year;
    VBox view;
    MovieInfo(String t, int y, String g, String p, String pu, String a, String s) {
        this.year = y;
        this.title = t;
        this.genres = new HashSet<String>(Arrays.asList(g.split(", ")));
        this.plot = p;
        this.posterURL = pu;
        this.absolutePath = a;
        this.subtitleAbsolutePath = s;
        this.view = null;
    }

    void setView(VBox v) {
        this.view = v;
    }
}