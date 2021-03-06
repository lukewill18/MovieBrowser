package main;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {
    private static final String fileRegex = "^([0-9]{4}) - (.*)\\..{3}$";

    private HashSet<String> allGenres;
    private FileManager fileManager;
    private NodeManager nodeManager;

    private FlowPane initRoot(Stage primaryStage, Multimap<String, MovieInfo> folderMap, ArrayList<MovieInfo> movieInfos) {
        primaryStage.setTitle("Movie Browser");
        primaryStage.getIcons().add(new Image("file:.style/icon.png"));

        FlowPane mainArea = new FlowPane();
        mainArea.getStyleClass().add("main-area");
        mainArea.setVgap(10);
        mainArea.setHgap(10);
        mainArea.setPadding(new Insets(15, 15, 15, 15));
        ScrollPane scrollPane = new ScrollPane(mainArea);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        nodeManager.setInCurrentFolder(movieInfos);
        ToolBar toolBar = nodeManager.generateToolBar(folderMap, movieInfos, allGenres);
        VBox layout = new VBox(toolBar, scrollPane);

        Scene scene = new Scene(layout, 600, 400);
        scene.getStylesheets().add("file:.style/style.css");
        primaryStage.setScene(scene);

        // No idea why this has to be done but it works
        primaryStage.setMaximized(false);
        primaryStage.setMaximized(true);
        return mainArea;
    }

    private void initWindow(Stage primaryStage, ArrayList<MovieInfo> movieInfos, Multimap<String, MovieInfo> folderMap) {
        FlowPane layout = initRoot(primaryStage, folderMap, movieInfos);

        for(MovieInfo movieInfo : movieInfos) {
            VBox imageViewWrapper = new VBox();
            imageViewWrapper.setPrefWidth(NodeManager.POSTER_WIDTH);

            ImageView poster = nodeManager.generatePoster(movieInfo);
            Label label = nodeManager.generatePosterLabel(movieInfo.title, movieInfo.year);

            if(movieInfo.plot.length() > 0) { // In case movie info is not found in database
                Tooltip plotSummary = new Tooltip(movieInfo.plot  + "\nIMDB Rating: " + movieInfo.imdbRating +
                        "\nDirector: " + movieInfo.director + "\nGenres: " + String.join(", ", movieInfo.genres)
                + "\nRuntime: " + movieInfo.runtime + " min");
                plotSummary.setPrefWidth(500);
                plotSummary.setShowDelay(Duration.millis(50));
                plotSummary.setShowDuration(Duration.INDEFINITE);
                Tooltip.install(imageViewWrapper, plotSummary);
            }
            nodeManager.makeWrapperClickable(imageViewWrapper, movieInfo);
            imageViewWrapper.getStyleClass().add("wrapper");
            imageViewWrapper.getChildren().addAll(poster, label);
            movieInfo.setView(imageViewWrapper);
            layout.getChildren().add(imageViewWrapper);
        }
        nodeManager.setInvisible(movieInfos);
    }

    private void displayLoading(Stage primaryStage) {
        primaryStage.setTitle("Movie Browser");
        primaryStage.getIcons().add(new Image("file:.style/icon.png"));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        Scene scene = new Scene(progressIndicator, 600, 400);
        scene.getStylesheets().add("file:.style/style.css");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaximized(true);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        displayLoading(primaryStage);
        allGenres = new HashSet<>();
        fileManager = new FileManager();
        nodeManager = new NodeManager(fileManager, primaryStage);
        fileManager.createCacheIfNoneExists();

        // Check if VLC location is specified
        fileManager.initVLC(primaryStage);

        // Maps subtitle name to absolute path
        HashMap<String, String> subtitleFilePaths = new HashMap<>();
        Collection<File> subtitleFiles = fileManager.getSubtitleFiles();
        for(File f : subtitleFiles) {
            subtitleFilePaths.put(f.getName().replaceFirst("[.][^.]+$", "").trim().toLowerCase(),
                    f.getAbsolutePath());
        }

        // Browse directory for movie files and find their info
        Collection<File> files = fileManager.getMovieFiles();
        ArrayList<MovieInfo> movieInfos = new ArrayList(files.size());
        Multimap<String, MovieInfo> folderMap = LinkedListMultimap.create();
        for(File f : files) {
            Pattern r = Pattern.compile(fileRegex);
            Matcher m = r.matcher(f.getName());
            if (m.find()) { // This is a valid movie file
                MovieInfo movieInfo = fileManager.getMovieFileInfo(f, subtitleFilePaths.get(f.getName()
                        .replaceFirst("[.][^.]+$", "").trim().toLowerCase()), m.group(1),
                        m.group(2).replaceAll(" ", "+"), m.group(2));

                if(movieInfo != null) {
                    movieInfos.add(movieInfo);
                    allGenres.addAll(movieInfo.genres);
                    folderMap.put(f.getParentFile().getName(), movieInfo);
                }
            } else {
                System.out.println("NO MATCH FOR FILE:");
                System.out.println(f.getName());
            }
        }
        initWindow(primaryStage, movieInfos, folderMap);
        nodeManager.setVisible(movieInfos);
    }

    public static void main(String[] args) {
        launch(args);
    }
}