package com.ocrproject;

import java.io.File;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    //TODO: Add logic to let the user decide where to put temp files and make variables accordingly
    String tempPreprocessImagePath = "/home/kagit/projects/java2/ocrproject/temp_preprocessed.png";
    String tempDownsizeImagePath = "/home/kagit/projects/java2/ocrproject/temp_downsized.png";
    Alert unimplementNotification = new Alert(Alert.AlertType.ERROR);
    CloseableHttpClient httpClient = HttpClients.createDefault();

    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage){
        stage.setTitle("Test Application");
        
        unimplementNotification.setTitle("Error");
        unimplementNotification.setHeaderText("Feature Not Implemented!");
        unimplementNotification.setContentText("Come back here at a later time.");

        tesseract.setLanguage("tur");
        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
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
            if(selectedFile != null){    
                if (tesseractCheckbox.isSelected()){
                    long tesseractTotalTimer = startTimer();
                    try{
                        long preprocessingTimer = startTimer();
                        File ProcessedImage = preprocessImage();
                        endTimer(preprocessingTimer, "Tesseract Preprocessing");
                        long tesseractTimer = startTimer();
                        ocrOutput = tesseract.doOCR(ProcessedImage);
                        endTimer(tesseractTimer, "Tesseract Processing");
                        endTimer(tesseractTotalTimer, "Tesseract Total Time");
                        rightPaneText.setText(ocrOutput); 
                    }
                        catch (TesseractException e1){
                            e1.printStackTrace();
                    }
                }
                if (placeholderCheckbox.isSelected()){
                    unimplementNotification.showAndWait();
                } 
                if (onlineApiCheckbox.isSelected()){
                    try {
                        System.out.println(testFunc(downsizeImage(selectedFile))); 
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else{
                    unimplementNotification.showAndWait();
                } 
            }
            else if(selectedFile == null){
                selectFilePopup.showAndWait();
            }
        });
        topPaneButtons.getChildren().addAll(selectFileButton, scanButton, tesseractCheckbox, placeholderCheckbox, onlineApiCheckbox);
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

    private String testFunc(File downsizedImage) throws Exception{
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
        //TODO: REMOVE HARDCODED API KEY AND ADD SETTING TO LET USER ADD IT INSTEAD
        builder.addTextBody("apikey", "NO KEY");
        builder.addTextBody("language", "tur");
        httpPost.setEntity(builder.build());

        ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null);
        String jsonResponse = EntityUtils.toString(response.getEntity());

        //TODO: Parse the JSON properly instead of outputting it straight
        JSONObject jsonObject = new JSONObject(jsonResponse);
        System.out.println(jsonResponse);
        return jsonResponse;
     }
     
     //TODO:Add logic to determine if we need to downsize images (<1.5 MB)
     private File downsizeImage(File imageFile){
        String imageFileLocation = imageFile.getAbsolutePath();
        Mat input = Imgcodecs.imread(imageFileLocation);
        Mat output = new Mat();
        Imgproc.resize(input, output, new Size(0, 0), 0.8, 0.8, Imgproc.INTER_AREA);

        Imgcodecs.imwrite(tempDownsizeImagePath, output);
        File downsizedImage = new File(tempDownsizeImagePath);
        return downsizedImage;
     }
}