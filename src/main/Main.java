package main;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("control-bar");
        toolBar.getItems().add(new Label("Enclosing Folder:"));
        toolBar.getItems().add(nodeManager.generateEnclosingFolderComboBox(folderMap, movieInfos));
        toolBar.getItems().add(new Label("Sort By:"));
        toolBar.getItems().add(nodeManager.generateSortByComboBox());
        toolBar.getItems().add(new Label("Genre:"));
        toolBar.getItems().add(nodeManager.generateGenreComboBox(allGenres));

        toolBar.getItems().add(nodeManager.generateRandomButton());
        toolBar.getItems().add(nodeManager.generateDeselectAllButton());

        VBox layout = new VBox(toolBar, scrollPane);

        Scene scene = new Scene(layout, 600, 400);
        scene.getStylesheets().add("file:.style/style.css");
        primaryStage.setScene(scene);
        return mainArea;
    }

    private void initWindow(Stage primaryStage, ArrayList<MovieInfo> movieInfos, Multimap<String, MovieInfo> folderMap) {
        FlowPane layout = initRoot(primaryStage, folderMap, movieInfos);
        primaryStage.show();

        for(MovieInfo movieInfo : movieInfos) {
            VBox imageViewWrapper = new VBox();
            imageViewWrapper.setPrefWidth(NodeManager.POSTER_WIDTH);

            ImageView poster = nodeManager.generatePoster(movieInfo);
            Label label = nodeManager.generatePosterLabel(movieInfo.title, movieInfo.year);

            Tooltip plotSummary = new Tooltip(movieInfo.plot);
            plotSummary.setPrefWidth(500);
            Tooltip.install(imageViewWrapper, plotSummary);

            imageViewWrapper.getChildren().addAll(poster, label);
            movieInfo.setView(imageViewWrapper);
            layout.getChildren().add(imageViewWrapper);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        allGenres = new HashSet<>();
        fileManager = new FileManager();
        nodeManager = new NodeManager(fileManager);
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}