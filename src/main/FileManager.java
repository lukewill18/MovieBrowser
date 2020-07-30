package main;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.Collection;

class FileManager {
    private String VLCLocation = "";
    private static final String DATA_CACHE_DIR = ".movieBrowserCache";
    String VLC_PATH_CACHE;
    private static final String API_URL = "http://www.omdbapi.com/?t=%s&y=%s&apikey=8b79c8d6";

    FileManager() {
        VLC_PATH_CACHE = DATA_CACHE_DIR + String.format("/.%s-vlcPath.cache", getMacAddress());
    }

    String getMacAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(ip);
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
        return "";
    }
    Collection<File> getMovieFiles() {
        File dir = new File(".");
        return FileUtils.listFiles(
                dir,
                new String[]{"mkv", "mp4", "avi", "VOB"},
                true
        );
    }

    Collection<File> getSubtitleFiles() {
        File dir = new File(".");
        return FileUtils.listFiles(
                dir,
                new String[]{"srt", "sub"},
                true
        );
    }

    private void cacheResponse(String response, File cacheFile) {
        try {
            FileWriter fileWriter = new FileWriter(cacheFile);
            fileWriter.write(response);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchVLC(String absolutePath) {
        ProcessBuilder pb = new ProcessBuilder(VLCLocation, absolutePath);
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchVLC(String absolutePath, String subtitleAbsolutePath) {
        ProcessBuilder pb = new ProcessBuilder(VLCLocation, absolutePath, "--sub-file=" + subtitleAbsolutePath);
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void playMovie(String absolutePath, String subtitleAbsolutePath){
        if(subtitleAbsolutePath == null) {
            launchVLC(absolutePath);
        }
        else {
            launchVLC(absolutePath, subtitleAbsolutePath);
        }
    }

    void chooseVLCFile(File pathCache, Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Setup - Select VLC.exe");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        VLCLocation = selectedFile.getAbsolutePath();

        try {
            FileWriter fileWriter = new FileWriter(pathCache);
            fileWriter.write(VLCLocation);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void initVLC(Stage primaryStage) throws Exception {
        File vlcPathCached = new File(VLC_PATH_CACHE);
        if(vlcPathCached.exists() && !vlcPathCached.isDirectory()) {
            BufferedReader br = new BufferedReader(new FileReader(vlcPathCached));
            VLCLocation = br.readLine();
            if (VLCLocation == null) {
                chooseVLCFile(vlcPathCached, primaryStage);
            } else {
                VLCLocation = VLCLocation.trim();
            }
        }
        else {
            chooseVLCFile(vlcPathCached, primaryStage);
        }
    }

    void createCacheIfNoneExists() {
        File cacheDir = new File(DATA_CACHE_DIR);
        if(!cacheDir.exists() || !cacheDir.isDirectory()) {
            cacheDir.mkdir();
        }
    }

    private MovieInfo readCacheFile(File cacheFile, String absolutePath, String subtitleAbsolutePath) throws Exception {
        BufferedReader in = new BufferedReader(
                new FileReader(cacheFile));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject jsonResponse =  new JSONObject(response.toString());
        cacheResponse(response.toString(), cacheFile);
        if(jsonResponse.has("Error")) {
            return new MovieInfo(cacheFile.getName().substring(8, cacheFile.getName().length() - 9),
                    cacheFile.getName().substring(1, 5), "", "",  "file:.style/filenotfound.png",
                    absolutePath, subtitleAbsolutePath, "", "", 0);
        }
        return new MovieInfo(jsonResponse.getString("Title"), jsonResponse.getString("Year"),
                jsonResponse.getString("Genre"), jsonResponse.getString("Plot"),
                jsonResponse.getString("Poster"), absolutePath, subtitleAbsolutePath,
                jsonResponse.getString("imdbRating"), jsonResponse.getString("Director"),
                Integer.parseInt(jsonResponse.getString("Runtime").split(" ")[0]));
    }

    private MovieInfo sendAPIRequest(String year, String title, String originalTitle, String absolutePath,
                                     File cacheFile, String subtitleAbsolutePath) {
        try {
            URL url = new URL(String.format(API_URL, title, year));
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            cacheResponse(response.toString(), cacheFile);
            if(jsonResponse.has("Error")) {
                return new MovieInfo(originalTitle, year, "", "",  "file:.style/filenotfound.png",
                        absolutePath, subtitleAbsolutePath, "", "", 0);
            }
            return new MovieInfo(originalTitle, year, jsonResponse.getString("Genre"),
                    jsonResponse.getString("Plot"), jsonResponse.getString("Poster"), absolutePath,
                    subtitleAbsolutePath, jsonResponse.getString("imdbRating"), jsonResponse.getString("Director"),
                    Integer.parseInt(jsonResponse.getString("Runtime").split(" ")[0]));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    MovieInfo getMovieFileInfo(File f, String subtitleAbsolutePath, String year, String title,
                                    String originalTitle) throws Exception {
        File cacheFile = new File(DATA_CACHE_DIR + "/." + f.getName() + ".json");
        MovieInfo movieInfo = null;
        if(cacheFile.exists() && !cacheFile.isDirectory()) {
            movieInfo = readCacheFile(cacheFile, f.getAbsolutePath(), subtitleAbsolutePath);
        }
        else {
            movieInfo = sendAPIRequest(year, title, originalTitle, f.getAbsolutePath(), cacheFile,
                    subtitleAbsolutePath);
        }
        return movieInfo;
    }
}