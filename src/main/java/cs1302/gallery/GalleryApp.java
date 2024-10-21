package cs1302.gallery;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Separator;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.collections.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.KeyValue;
import javafx.util.Duration;
import java.lang.Exception;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.LinkedList;
import java.time.LocalTime;
import java.lang.Math;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    private final String itunesLink = "https://itunes.apple.com/search";

    private Stage stage;
    private Scene scene;
    private VBox root;

    private Button playButton;
    private ProgressBar progressBar;
    private ComboBox<String>  dropdown;
    private Button getImages;
    private HBox urlPane;
    private HBox loadBar;
    private HBox container;
    private HBox paneContainer;
    private TextField searchBar;
    private Text tx;
    private Text link;
    private Text msg;
    private TilePane display;
    private Insets searchBarPad;
    private Separator separator;
    private Insets linkPad;

    private String playAndPause = "Play";
    private String url;
    private ImageView[] imgView = new ImageView[20];
    private ObservableList<String> choices = FXCollections.observableArrayList(
        "movie", "podcast", "music", "musicVideo", "audiobook",
        "shortfilm", "tvShow", "software", "ebook", "all"
        );
    private String urlDefault = "Type in a term, select a media type, then click the button";
    private String msgDefault = "Images provided by iTunes search API";
    private int changer = 0;
    private int pP = 0;
    private Timeline timeline;
    private String prevResponse;
    private LinkedList<String> linked;



    /**
     * Constructs a {@code GalleryApp} object}.
     */
    public GalleryApp() {
        this.stage = null;
        this.scene = null;
        this.root = new VBox(3);
        this.urlPane = new HBox(2.5);
        this.loadBar = new HBox(2.5);
        this.container = new HBox();
        this.paneContainer = new HBox();
        this.searchBar = new TextField("");
        this.getImages = new Button("Get Images");
        this.playButton = new Button ("Play");
        this.dropdown = new ComboBox(choices);
        this.tx = new Text("Search:");
        this.link = new Text(urlDefault);
        this.msg = new Text(msgDefault);
        this.display = new TilePane();
        this.progressBar = new ProgressBar(0);

        this.searchBarPad = new Insets(3, 3, 3, 3);
        this.linkPad = new Insets(0, 0, 0, 3);
        this.separator = new Separator(Orientation.VERTICAL);
    } // GalleryApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        HBox.setHgrow(this.searchBar, Priority.ALWAYS);
        HBox.setHgrow(this.progressBar, Priority.ALWAYS);
        HBox.setHgrow(this.paneContainer, Priority.ALWAYS);

        this.urlPane.getChildren().addAll(
            this.playButton, this.separator, this.tx,
            this.searchBar, this.dropdown, this.getImages);
        this.urlPane.setPadding(searchBarPad);
        this.urlPane.setAlignment(Pos.CENTER_LEFT);
        this.tx.setLineSpacing(2.0);

        this.container.getChildren().addAll(this.link);
        this.container.setAlignment(Pos.CENTER_LEFT);
        this.container.setPadding(linkPad);

        this.paneContainer.getChildren().addAll(this.display);
        this.paneContainer.setAlignment(Pos.CENTER);

        this.loadBar.getChildren().addAll(this.progressBar, this.msg);
        this.loadBar.setPadding(searchBarPad);

        this.progressBar.setPrefWidth(260.0);
        this.dropdown.getSelectionModel().select(2);
        this.playButton.setDisable(true);
        this.display.setPrefColumns(5);
        for (int i = 0; i < 20; i++) {
            Image post = new Image("file:resources/default.png", 105.0, 105.0, false, true);
            this.display.getChildren().add(new ImageView(post));
        }

        this.root.getChildren().addAll(
            this.urlPane, this.container, this.paneContainer, this.loadBar);

        this.getImages.setOnAction(e -> {
            Thread t = new Thread(() -> this.gettingImages());
            t.setDaemon(true);
            t.start();
        });

        this.playButton.setOnAction(e -> {
            Thread p = new Thread(() -> this.playPause());
            p.setDaemon(true);
            p.start();
        });
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /**
     * This method takes in {@code button} which determines if it miust replace the images randomly
     * or if it must load images onto the screen via the {@code getImage} button.
     *
     * @return a LinkedList collection of all the unique image links.
     * @throw IOException if the status code is not 200.
     * @throw IllegalArgumentException is th e distinct number of images is not 21 or more.
     */
    public LinkedList<String> fetchSearch() {
        LinkedList<String> imageCollection = new LinkedList<String>();
        String error = "last attempt to get images failed...";
        String errortxt = " distinct results are found, but 21 or more are needed";
        try {
            String uri = createRequestLink();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (changer != 0) {
                    this.setProgress(1);
                    this.playButton.setDisable(true);
                }
                this.getImages.setDisable(false);
                Platform.runLater(() -> this.link.setText(error));
                throw new IOException(response.toString());
            }
            String jsonString = response.body();
            ItunesResponse itunesResponse = GSON.fromJson(jsonString, ItunesResponse.class);
            if (itunesResponse.resultCount < 21) {
                if (changer != 0) {
                    this.setProgress(1);
                    this.playButton.setDisable(false);
                }
                this.getImages.setDisable(false);
                Platform.runLater(() -> this.link.setText(error));
                String num = " " + itunesResponse.resultCount;
                throw new IllegalArgumentException(num + errortxt);
            }
            int tot = itunesResponse.resultCount;
            this.setProgress(0);
            for (int i = 0; i < itunesResponse.results.length; i++) {
                ItunesResult result = itunesResponse.results[i];
                if (!(imageCollection.contains(result.artworkUrl100))) {
                    imageCollection.add(result.artworkUrl100);
                    double progress = (1.0 * i) / (tot * 1.0);
                    this.setProgress(progress);
                    Thread.sleep((int) Math.round(Math.random() * (tot * 4)));
                }
            }
            if (imageCollection.size() < 21) {
                if (changer != 0) {
                    this.setProgress(1);
                    this.playButton.setDisable(false);
                }
                this.getImages.setDisable(false);
                Platform.runLater(() -> this.link.setText(error));
                String num = " " + imageCollection.size();
                throw new IllegalArgumentException(num + errortxt);
            }
            this.setProgress(1);
            return imageCollection;
        } catch (Exception e) {
            Platform.runLater(() -> alertError(e, createRequestLink()));
            return imageCollection;
        }
    }

    /**
     * Called when {@code getImage} button is clicked
     * Does not get work if less than 21 items in LinkedList
     * Disables {@code getImages} and {@code playButton} buttons
     * Loads images if no errors thrown
     * Catches any errors.
     */
    public void gettingImages() {
        try {
            Platform.runLater(() -> this.link.setText("Getting images..."));
            this.getImages.setDisable(true);
            this.playButton.setDisable(true);
            if (timeline != null) {
                this.timeline.stop();
                Platform.runLater(() -> this.playButton.setText("Play"));
            }
            this.setProgress(0);
            LinkedList<String> links = fetchSearch();
            if (links.size() < 21) {
                String failed = "failed";
            } else {
                changer++;
                int count = 0;
                while (count != 20) {
                    final int a = count;
                    final Image post = new Image(links.get(a), 105.0, 105.0, false, true);
                    final ImageView img = new ImageView(post);
                    Platform.runLater(() -> this.display.getChildren().remove(a));
                    Platform.runLater(() -> this.display.getChildren().add(a, img));
                    count++;
                }
                Platform.runLater(() -> this.link.setText(createRequestLink()));
                this.playButton.setDisable(false);
                this.getImages.setDisable(false);
                this.linked = links;
            }
        } catch (Exception e) {
            Platform.runLater(() -> alertError(e, createRequestLink()));
        }
    }

    /**
     * Called when the {@code playButton} is clicked
     * Keeps track of whether the {@code playButton} is supposed to be "Paused"
     * or "Play"
     * Calls a method using a lambda to indefinetly execute a method until stopped
     * Uses {@code timeline} and {@code KeyFrame} and {@code KeyValue} to indefinetly
     * replace images until {@code playButton} is clicked
     * When button is clicked with "Paused" displayed, {@code timeline} is stopped.
     */
    public void playPause() {
        int keepTrack = pP;
        int count = 0;
        if ((keepTrack % 2) == 0) {
            timeline = new Timeline();
            LinkedList<String> links = linked;
            EventHandler<ActionEvent> played = event -> {
                createHavoc(links);
            };
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), played);
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.getKeyFrames().add(keyFrame);
            timeline.play();

            pP = 1;
            Platform.runLater(() -> this.playButton.setText("Pause"));
        } else if ((keepTrack % 2) == 1) {
            timeline.stop();
            pP = 0;
            Platform.runLater(() -> this.playButton.setText("Play"));
        }
    }

    /**
     * Replaces random parts of the {@code display} with different images
     * that have not been used yet.
     *
     * @param links takes the distinct img url list from clicking the {@code getImages}.
     */
    public void createHavoc(LinkedList<String> links) {
        int pick = 20 + (int) Math.round(Math.random() * (links.size() - 21));
        int change = (int) Math.round(Math.random() * 19);

        String keeper = links.get(pick);
        links.set(pick, links.get(change));
        links.set(change, keeper);

        if ((links.get(change)).equals(keeper)) {
            Image post = new Image(links.get(change), 105.0, 105.0, false, true);
            ImageView img = new ImageView(post);
            Platform.runLater(() -> this.display.getChildren().remove(change));
            Platform.runLater(() -> this.display.getChildren().add(change, img));
        }
    }

    /**
     * Updates the {@code progressBar} by taking in {@code progress}.
     *
     * @param progress takes in a double value that sets the progress on the {@code progressBar}.
     */
    private void setProgress(final double progress) {
        Platform.runLater(() -> this.progressBar.setProgress(progress));
    }

    /**
     * Gets the item requested by {@code searchBar}
     * Also gets requested type of item from {@code dropdown}
     * Creates a uri string to return using {@code itunesApi} and formatted info.
     *
     * @return a String with created uri
     */
    public String createRequestLink() {
        String text = this.searchBar.getText();

        String med = this.dropdown.getValue().toString();
        String term = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String media = URLEncoder.encode(med, StandardCharsets.UTF_8);
        String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
        String query = String.format("?term=%s&media=%s&limit=%s", term, media, limit);
        String uri = itunesLink + query;

        return uri;
    }

    /**
     * Takes in {@code cause} which is the type of error
     * Takes in {@code URI} to see where error occured
     * Creates an error message to display
     * Created message displayed on a {@code Alert} window.
     *
     * @param cause the Exception that was caught
     * @param URI the link that caused the error
     */
    public static void alertError(Throwable cause, String URI) {
        TextArea text = new TextArea("URI: " + URI + "\n\n" +
            "Exception: " + cause.toString());
        text.setEditable(false);
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        // feel free to modify this method
        System.out.println("stop() called");
    } // stop

} // GalleryApp
