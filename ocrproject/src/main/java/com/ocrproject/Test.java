package com.ocrproject;

import java.io.File;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import nu.pattern.OpenCV;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.json.JSONObject;

public class Test extends Application {
    FileChooser fileChooser = new FileChooser();
    File selectedFile = null;
    Label topPaneLabel = null;
    ImageView image = new ImageView();
    TextArea rightPaneText = new TextArea("Test Text");
    Tesseract tesseract = new Tesseract();
    String ocrOutput = null;
    String tempDir = System.getProperty("java.io.tmpdir");
    String tempPreprocessImagePath = tempDir + "/temp_preprocessed.png";
    String tempDownsizeImagePath = tempDir + "/temp_downsized.png";
    String tesseractDataPath = "/usr/share/tesseract-ocr/5/tessdata";
    String apiKey;
    Alert unimplementNotification = new Alert(Alert.AlertType.ERROR);
    Alert apiKeyNotSetNotification = new Alert(Alert.AlertType.ERROR);
    CloseableHttpClient httpClient = HttpClients.createDefault();

    public static void main(String[] args) {
        launch(args);
    }
    
    public void setAlerts(){
        unimplementNotification.setTitle("Error");
        unimplementNotification.setHeaderText("Feature Not Implemented!");
        unimplementNotification.setContentText("Come back here at a later time.");

        apiKeyNotSetNotification.setTitle("Error");
        apiKeyNotSetNotification.setHeaderText("You haven't set the API key!");
        apiKeyNotSetNotification.setContentText("Please go to settings and type in a API key.");
    }
    
    @Override
    public void start(Stage stage){
        stage.setTitle("Test Application");
        
        System.out.println(tempDir);

        setAlerts();

        tesseract.setLanguage("tur");
        tesseract.setDatapath(tesseractDataPath);
        OpenCV.loadLocally();

        BorderPane borderPane = new BorderPane();
        
        VBox topPane = getTopPane();
        borderPane.setTop(topPane);

        VBox centerPane = getCenterPane();
        borderPane.setLeft(centerPane);

        VBox rightPane = getRightPane();
        borderPane.setRight(rightPane);

        Scene scene = new Scene(borderPane, 650, 350, Color.BEIGE);
        stage.setScene(scene);
        
        stage.show();
    }

    public VBox getRightPane() {
        VBox rightPane = new VBox(10);
        //rightPane.setPrefWidth(200);
        rightPane.setPadding(new Insets(10));
        rightPane.setMaxWidth(300);
        rightPane.getChildren().addAll(rightPaneText);
        return rightPane;
    }

    public VBox getCenterPane() {
        VBox centerPane = new VBox(10);
        centerPane.setPadding(new Insets(10));

        image.setFitWidth(300);
        image.setPreserveRatio(true);

        centerPane.getChildren().addAll(image);
        return centerPane;
    }

    public VBox getTopPane() {
        VBox topPane = new VBox(10);
        HBox topPaneButtons = new HBox(10);
        topPane.setPadding(new Insets(10));
        Button selectFileButton = new Button("Select File");        
        Button scanButton = new Button("Scan");
        Button settingsButton = new Button("Settings");
        topPaneLabel = new Label("Please select a file.");
        CheckBox tesseractCheckbox = new CheckBox("Tesseract");
        tesseractCheckbox.setPadding(new Insets(3.5));
        CheckBox placeholderCheckbox = new CheckBox("Placeholder");
        placeholderCheckbox.setPadding(new Insets(3.5));
        CheckBox onlineApiCheckbox = new CheckBox("Online API");
        onlineApiCheckbox.setPadding(new Insets(3.5));
        Alert selectFilePopup = new Alert(Alert.AlertType.INFORMATION);
        selectFilePopup.setTitle("File Selection");
        selectFilePopup.setHeaderText("You haven't selected a file.");
        selectFilePopup.setContentText("Please select a file, then try again.");
        selectFileButton.setOnAction (e -> {
            selectFile();
        });
        scanButton.setOnAction (e -> {
            //TODO: Eye-watering logic.
            if(selectedFile != null){    
                if (tesseractCheckbox.isSelected()){
                    doTesseractOCR();
                }
                if (placeholderCheckbox.isSelected()){
                    unimplementNotification.showAndWait();
                } 
                if (onlineApiCheckbox.isSelected()){
                    try {
                        System.out.println(callAPI(downsizeImageOrSkip(selectedFile))); 
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                if (tesseractCheckbox.isSelected() != true && placeholderCheckbox.isSelected() != true && onlineApiCheckbox.isSelected() != true){
                    unimplementNotification.showAndWait();
                } 
            }
            else if(selectedFile == null){
                selectFilePopup.showAndWait();
            }
        });
        settingsButton.setOnAction(e -> {
            openSettingsWindow();
        });
        topPaneButtons.getChildren().addAll(
            selectFileButton, scanButton, settingsButton,
            tesseractCheckbox, placeholderCheckbox, onlineApiCheckbox);
        
            topPane.getChildren().addAll(topPaneButtons, topPaneLabel);
        return topPane;
    }

    public void selectFile() {
        fileChooser.setTitle("Select an Image");
        FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter("Image Files (*.jpg, *.png)", "*.jpg", "*.png");
        fileChooser.getExtensionFilters().add(fileFilter);      
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile !=null){
            topPaneLabel.setText("Selected: " + selectedFile.getName());
            Image selectedImage = new Image(selectedFile.toURI().toString());
            image.setImage(selectedImage);
        }
    }

    public File preprocessImage() throws TesseractException {
        Mat matImage = Imgcodecs.imread(selectedFile.getAbsolutePath());
        Mat bwImage = new Mat();
        Imgproc.cvtColor(matImage, bwImage, Imgproc.COLOR_BGR2GRAY);

        //Mat denoised = new Mat();
        //Imgproc.bilateralFilter(bwImage, denoised, 9, 50, 50);
        
        Mat binaryImage = new Mat();
        Imgproc.threshold(bwImage, binaryImage, 110, 255, Imgproc.THRESH_BINARY);
        Imgcodecs.imwrite(tempPreprocessImagePath, binaryImage);
        
        File processedFile = new File(tempPreprocessImagePath);
        System.out.println(processedFile.getAbsolutePath());
        System.out.println("Image size: " + matImage.width() + "x" + matImage.height());
        System.out.println("Channels: " + matImage.channels());        
        return processedFile;
    }

    private long startTimer(){
        return System.currentTimeMillis();
    }

    private long endTimer(long startTime, String label){
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(label + ": " + elapsed + "ms");
        return elapsed;
    }

    private String callAPI(File downsizedImage) throws Exception{
        String jsonResponse = "No response or no API key set.";
        if (apiKey.isBlank()){
            apiKeyNotSetNotification.showAndWait();
            return jsonResponse;
        }
        
        HttpPost httpPost = new HttpPost("https://api.ocr.space/parse/image");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String fileName = downsizedImage.getName().toLowerCase();
        ContentType contentType = null;
        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")){
            contentType = ContentType.IMAGE_JPEG;
        }
        else if(fileName.endsWith("png")){
            contentType = ContentType.IMAGE_PNG;
        }
        else {
            contentType = ContentType.APPLICATION_OCTET_STREAM;
        }

        builder.addBinaryBody("file", downsizedImage, contentType, downsizedImage.getName());
        builder.addTextBody("apikey", apiKey);
        System.out.println("trying api key: " + apiKey);
        builder.addTextBody("language", "tur");
        httpPost.setEntity(builder.build());

        ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null);
        jsonResponse = EntityUtils.toString(response.getEntity());

        //TODO: Parse the JSON properly instead of outputting it straight
        JSONObject jsonObject = new JSONObject(jsonResponse);
        System.out.println(jsonResponse);
        return jsonResponse;
     }
     
     private File downsizeImageOrSkip(File imageFile){
        File downsizedImage = imageFile;
        System.out.println("image computed size: " + ((float)imageFile.length() / (1024* 1024)));
        if ((float)imageFile.length() / (1024* 1024) > 1.5){
            String imageFileLocation = imageFile.getAbsolutePath();
            Mat input = Imgcodecs.imread(imageFileLocation);
            Mat output = new Mat();
            Imgproc.resize(input, output, new Size(0, 0), 0.8, 0.8, Imgproc.INTER_AREA);

            Imgcodecs.imwrite(tempDownsizeImagePath, output);
            downsizedImage = new File(tempDownsizeImagePath);
        }
        return downsizedImage;
     }

     private void doTesseractOCR(){
        long tesseractTotalTimer = startTimer();
        try{
            long preprocessingTimer = startTimer();
            File processedImage = preprocessImage();
            endTimer(preprocessingTimer, "Tesseract Preprocessing");
            long tesseractTimer = startTimer();
            ocrOutput = tesseract.doOCR(processedImage);
            endTimer(tesseractTimer, "Tesseract Processing");
            endTimer(tesseractTotalTimer, "Tesseract Total Time");
            rightPaneText.setText(ocrOutput); 
            }
            catch (TesseractException e1){
                e1.printStackTrace();
            }
     }
     private void openSettingsWindow(){
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Settings");
        settingsStage.setWidth(400);
        settingsStage.setHeight(300);

        VBox settingsPane = new VBox(10);
        settingsPane.setPadding(new Insets(15));
        Label tesseractPathLabel = new Label("Tesseract Data Path: ");
        TextField tesseractPathField = new TextField(tesseractDataPath);
        tesseractPathField.setPrefWidth(250);
        Button tesseractPathButton = new Button("Browse...");
        tesseractPathButton.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Please select Tesseract's data directory");
            File selectedDir = dirChooser.showDialog(settingsStage);
            if (selectedDir != null){
                tesseractPathField.setText(selectedDir.getAbsolutePath());
            }
        });

        HBox tesseractPathBox = new HBox(10);
        tesseractPathBox.getChildren().addAll(tesseractPathField, tesseractPathButton);

        Label tempPathLabel = new Label("Temp Image Path:");
        TextField tempPathField = new TextField();
        tempPathField.setPrefWidth(250);
        tempPathField.setText(tempPreprocessImagePath);
        Button tempPathButton = new Button("Browse...");
        tempPathButton.setOnAction(e -> {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Temp Image Directory");
        File selectedDir = dirChooser.showDialog(settingsStage);
        
        if (selectedDir != null) {
            tempPathField.setText(selectedDir.getAbsolutePath());
        }
        });
    
        HBox tempPathBox = new HBox(10);
        tempPathBox.getChildren().addAll(tempPathField, tempPathButton);
    
        Label apiKeyLabel = new Label ("OCR API Key:");
        TextField apiKeyField = new TextField();
        apiKeyField.setPrefWidth(250);
        apiKeyField.setText(apiKey);

        HBox apiKeyBox = new HBox(10);
        apiKeyBox.getChildren().addAll(apiKeyField);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            tesseract.setDatapath(tesseractPathField.getText());
            tempPreprocessImagePath = tempPathField.getText();
            apiKey = apiKeyField.getText();
            settingsStage.close();
        });
    
        settingsPane.getChildren().addAll(
            tesseractPathLabel, tesseractPathBox,
            tempPathLabel, tempPathBox,
            apiKeyLabel, apiKeyBox,
            saveButton
        );
    
        Scene settingsScene = new Scene(settingsPane);
        settingsStage.setScene(settingsScene);
        settingsStage.show();
    }
    
    @Override
     public void stop() throws Exception{
        httpClient.close();
        System.out.println("debug: http client closed");
        super.stop();
     }
}