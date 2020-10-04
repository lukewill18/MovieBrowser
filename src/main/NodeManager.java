package main;

import com.google.common.collect.Multimap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

class NodeManager {
    static class yearAscendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.year.compareTo(movieInfo1.year);
        }
    }

    static class yearDescendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo1.year.compareTo(movieInfo2.year);
        }
    }

    static class titleComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.title.compareTo(movieInfo1.title);
        }
    }

    static class imdbRatingAscendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.imdbRating.compareTo(movieInfo1.imdbRating);
        }
    }

    static class imdbRatingDescendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo1.imdbRating.compareTo(movieInfo2.imdbRating);
        }
    }

    static class runtimeAscendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo2.runtime - movieInfo1.runtime;
        }
    }
    static class runtimeDescendingComparator implements Comparator<MovieInfo> {
        @Override
        public int compare(MovieInfo movieInfo1, MovieInfo movieInfo2) {
            return movieInfo1.runtime - movieInfo2.runtime;
        }
    }

    static final int POSTER_WIDTH = 150;
    private FileManager fileManager;

    private Comparator<MovieInfo> currentComparator;
    private List<MovieInfo> inCurrentFolder;
    private List<MovieInfo> currentlyDisplayed;

    private String currentGenre;
    private HashSet<MovieInfo> randomPool;
    private Stage primaryStage;

    NodeManager(FileManager f, Stage p) {
        currentComparator = new yearAscendingComparator();
        currentGenre = "All";
        randomPool = new HashSet<>();
        inCurrentFolder = new ArrayList<>();
        currentlyDisplayed = new ArrayList<>();
        fileManager = f;
        primaryStage = p;
    }

    void setInCurrentFolder(List<MovieInfo> movieInfos) {
        inCurrentFolder = movieInfos;
    }

    void setInvisible(List<MovieInfo> movieInfos) {
        for (MovieInfo movieInfo : movieInfos) {
            if (movieInfo.view != null) {
                movieInfo.view.setVisible(false);
                movieInfo.view.setManaged(false);
            }
        }
    }

    void setVisible(List<MovieInfo> movieInfos) {
        movieInfos.sort(currentComparator);
        for (MovieInfo movieInfo : movieInfos) {
            if (movieInfo.view != null) {
                movieInfo.view.setVisible(true);
                movieInfo.view.setManaged(true);
                movieInfo.view.toBack();
            }
        }
        currentlyDisplayed = movieInfos;
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

    private ComboBox<String> generateEnclosingFolderComboBox(Multimap<String, MovieInfo> folderMap,
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

    private ComboBox<String> generateSortByComboBox() {
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.getItems().addAll("Year (Low to High)", "Year (High to Low)", "Title",
                "IMDB Rating (Low to High)", "IMDB Rating (High to Low)", "Runtime (Low to High)",
                "Runtime (High to Low)");
        EventHandler<ActionEvent> event =
            e -> {
                String selected = comboBox.getValue();
                if (selected.equals("Year (Low to High)")) {
                    currentComparator = new yearAscendingComparator();
                }
                else if(selected.equals("Year (High to Low)")) {
                    currentComparator = new yearDescendingComparator();
                }
                else if(selected.equals("Title")){
                    currentComparator = new titleComparator();
                }
                else if(selected.equals("IMDB Rating (Low to High)")){
                    currentComparator = new imdbRatingAscendingComparator();
                }
                else if(selected.equals("IMDB Rating (High to Low)")){
                    currentComparator = new imdbRatingDescendingComparator();
                }
                else if(selected.equals("Runtime (Low to High)")) {
                    currentComparator = new runtimeAscendingComparator();
                }
                else {
                    currentComparator = new runtimeDescendingComparator();
                }
                setInvisible(inCurrentFolder);
                List<MovieInfo> toShow = filterGenre(inCurrentFolder, currentGenre);
                setVisible(toShow);
            };

        comboBox.setOnAction(event);
        comboBox.getSelectionModel().selectFirst();;
        return comboBox;
    }


    private ComboBox<String> generateGenreComboBox(Set<String> allGenres) {
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

    private Button generateRandomButton() {
        Button button = new Button("Play Random");
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());

        button.setOnAction(event -> {
            MovieInfo movieInfo = null;
            if(randomPool.isEmpty()) {
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

    private Button generateDeselectAllButton() {
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
        if(image.getWidth() == 0) { // URL is bad
            image = new Image("file:.style/filenotfound.png");
        }
        ImageView poster = new ImageView();
        poster.setImage(image);
        poster.setFitWidth(POSTER_WIDTH);
        poster.setPreserveRatio(true);
        poster.setSmooth(true);
        poster.setCache(true);

        return poster;
    }

    void makeWrapperClickable(VBox imageView, MovieInfo movieInfo) {
        imageView.setOnMouseClicked((MouseEvent e) -> {
            if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                fileManager.playMovie(movieInfo.absolutePath, movieInfo.subtitleAbsolutePath);
            }
            else {
                if (!randomPool.contains(movieInfo)) {
                    randomPool.add(movieInfo);
                    movieInfo.view.getStyleClass().add("glowing");
                } else {
                    randomPool.remove(movieInfo);
                    movieInfo.view.getStyleClass().remove("glowing");
                }
            }
        });
    }

    Label generatePosterLabel(String title, String year) {
        Label label = new Label(title + " (" + year + ")");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private Button generateLocateVLCButton() {
        Button button = new Button("Set VLC Location");
        button.setOnAction(event -> {
            try {
                fileManager.chooseVLCFile(new File(fileManager.VLC_PATH_CACHE), primaryStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return button;
    }

    private TextField generateSearchBar() {
        TextField textBox = new TextField();
        textBox.setPromptText("Search");
        textBox.setOnKeyReleased(e -> {
            KeyCode keycode = e.getCode();
            if (keycode == KeyCode.ENTER){
                String query = textBox.getText().trim().toLowerCase();
                List<MovieInfo> candidates = filterGenre(inCurrentFolder, currentGenre);
                if(query.isEmpty()) {
                    setVisible(candidates);
                }
                else {
                    setInvisible(inCurrentFolder);
                    Set<String> searchTerms = new HashSet<>(Arrays.asList((query.split(" "))));
                    List<MovieInfo> results = new ArrayList<>();

                    for(MovieInfo movieInfo : candidates) {
                        if(movieInfo.tags.containsAll(searchTerms)) {
                            results.add(movieInfo);
                        }
                    }
                    setVisible(results);
                }
            }
            else if(keycode == KeyCode.BACK_SPACE || keycode == KeyCode.DELETE) {
                if(textBox.getText().trim().isEmpty()) {
                    setVisible(filterGenre(inCurrentFolder, currentGenre));
                }
            }
        });

        return textBox;
    }

    ToolBar generateToolBar(Multimap<String, MovieInfo> folderMap, ArrayList<MovieInfo> movieInfos,
                            Set<String> allGenres) {
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("control-bar");
        ObservableList<Node> toolBarItems = toolBar.getItems();
        toolBarItems.add(new Label("Enclosing Folder:"));
        toolBarItems.add(generateEnclosingFolderComboBox(folderMap, movieInfos));
        toolBarItems.add(new Label("Sort By:"));
        toolBarItems.add(generateSortByComboBox());
        toolBarItems.add(new Label("Genre:"));
        toolBarItems.add(generateGenreComboBox(allGenres));

        toolBarItems.add(generateRandomButton());
        toolBarItems.add(generateDeselectAllButton());
        toolBarItems.add(generateSearchBar());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolBarItems.add(spacer);
        toolBarItems.add(generateLocateVLCButton());

        return toolBar;
    }
}
