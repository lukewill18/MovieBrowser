package main;

import com.google.common.collect.Multimap;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class NodeManager {
    static class yearAscendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.year - movieInfo1.year;
        }
    }

    static class yearDescendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo1.year - movieInfo2.year;
        }
    }

    static class titleComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.title.compareTo(movieInfo1.title);
        }
    }

    static final int POSTER_WIDTH = 150;
    private FileManager fileManager;

    private Comparator<MovieInfo> currentComparator;
    private List<MovieInfo> inCurrentFolder = new ArrayList<>();
    private String currentGenre;
    private HashSet<MovieInfo> randomPool;

    NodeManager(FileManager f) {
        currentComparator = new yearDescendingComparator();
        currentGenre = "All";
        randomPool = new HashSet<>();
        fileManager = f;
    }

    void setInCurrentFolder(List<MovieInfo> movieInfos) {
        inCurrentFolder = movieInfos;
    }

    private void setInvisible(List<MovieInfo> movieInfos) {
        for (MovieInfo movieInfo : movieInfos) {
            if (movieInfo.view != null) {
                movieInfo.view.setVisible(false);
                movieInfo.view.setManaged(false);
                movieInfo.view.toBack();
            }
        }
    }

    private void setVisible(List<MovieInfo> movieInfos) {
        movieInfos.sort(currentComparator);
        for (MovieInfo movieInfo : movieInfos) {
            if (movieInfo.view != null) {
                movieInfo.view.setVisible(true);
                movieInfo.view.setManaged(true);
                movieInfo.view.toBack();
            }
        }
    }

    private List<MovieInfo> filterGenre(Collection<MovieInfo> toFilter, String genre) {
        if(genre.equals("All")) {
            return new ArrayList<>(toFilter);
        }
        List<MovieInfo> filtered = new ArrayList<>(toFilter.size());
        for(MovieInfo movieInfo : toFilter) {
            if(movieInfo.genres.contains(genre)) {
                filtered.add(movieInfo);
            }
        }
        return filtered;
    }

    ComboBox<String> generateEnclosingFolderComboBox(Multimap<String, MovieInfo> folderMap,
                                                     ArrayList<MovieInfo> movieInfos) {
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.getItems().add("All");
        comboBox.getItems().addAll(FXCollections.observableArrayList(folderMap.keySet()));
        EventHandler<ActionEvent> event =
            e -> {
                String selected = comboBox.getValue();
                setInvisible(movieInfos);
                if (selected.equals("All")) {
                    inCurrentFolder = movieInfos;
                    List<MovieInfo> toShow = filterGenre(inCurrentFolder, currentGenre);
                    setVisible(toShow);
                } else {
                    inCurrentFolder = new ArrayList<>(folderMap.get(selected));
                    List<MovieInfo> toShow = filterGenre(inCurrentFolder, currentGenre);
                    setVisible(toShow);
                }
            };
        comboBox.setOnAction(event);
        comboBox.getSelectionModel().selectFirst();
        return comboBox;
    }

    ComboBox<String> generateSortByComboBox() {
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.getItems().addAll("Year (Ascending)", "Year (Descending)", "Title");
        EventHandler<ActionEvent> event =
            e -> {
                String selected = comboBox.getValue();
                if (selected.equals("Year (Ascending)")) {
                    currentComparator = new yearAscendingComparator();
                }
                else if(selected.equals("Year (Descending)")) {
                    currentComparator = new yearDescendingComparator();
                }
                else {
                    currentComparator = new titleComparator();
                }
                setInvisible(inCurrentFolder);
                List<MovieInfo> toShow = filterGenre(inCurrentFolder, currentGenre);
                setVisible(toShow);
            };

        comboBox.setOnAction(event);
        // Initially sort by year (ascending)
        comboBox.getSelectionModel().selectFirst();
        setVisible(inCurrentFolder);
        return comboBox;
    }


    ComboBox<String> generateGenreComboBox(Set<String> allGenres) {
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.getItems().add("All");
        List<String> sortedGenres = new ArrayList<>(allGenres);
        Collections.sort(sortedGenres);
        comboBox.getItems().addAll(sortedGenres);

        EventHandler<ActionEvent> event = e -> {
            currentGenre = comboBox.getValue();
            if(currentGenre.equals("All")) {
                setVisible(inCurrentFolder);
            }
            else {
                setInvisible(inCurrentFolder);
                List<MovieInfo> toShow = filterGenre(inCurrentFolder, currentGenre);
                setVisible(toShow);
            }
        };

        comboBox.setOnAction(event);
        comboBox.getSelectionModel().selectFirst();
        return comboBox;
    }

    Button generateRandomButton() {
        Button button = new Button("Play Random");
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());

        button.setOnAction(event -> {
            MovieInfo movieInfo = null;
            if(randomPool.isEmpty()) {
                List<MovieInfo> currentlyDisplayed = filterGenre(inCurrentFolder, currentGenre);
                if(!currentlyDisplayed.isEmpty()) {
                    movieInfo = currentlyDisplayed.get(random.nextInt(currentlyDisplayed.size()));
                }
            }
            else {
                List<MovieInfo> poolList = new ArrayList<>(randomPool);
                movieInfo = poolList.get(random.nextInt(poolList.size()));
            }
            if(movieInfo != null) {
                fileManager.playMovie(movieInfo.absolutePath, movieInfo.subtitleAbsolutePath);
            }
        });
        return button;
    }

    Button generateDeselectAllButton() {
        Button button = new Button("Deselect All");
        button.setOnAction(event -> {
            for(MovieInfo movieInfo : randomPool) {
                movieInfo.view.getStyleClass().remove("glowing");
            }
            randomPool.clear();
        });
        return button;
    }

    ImageView generatePoster(MovieInfo movieInfo) {
        Image image = new Image(movieInfo.posterURL);
        ImageView poster = new ImageView();
        poster.setImage(image);
        poster.setFitWidth(POSTER_WIDTH);
        poster.setPreserveRatio(true);
        poster.setSmooth(true);
        poster.setCache(true);

        poster.getStyleClass().add("poster");
        poster.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!randomPool.contains(movieInfo)) {
                    randomPool.add(movieInfo);
                    movieInfo.view.getStyleClass().add("glowing");
                } else {
                    randomPool.remove(movieInfo);
                    movieInfo.view.getStyleClass().remove("glowing");
                }
            }
            else {
                fileManager.playMovie(movieInfo.absolutePath, movieInfo.subtitleAbsolutePath);
            }

        });
        return poster;
    }

    Label generatePosterLabel(String title, int year) {
        Label label = new Label(title + " (" + year + ")");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }
}
