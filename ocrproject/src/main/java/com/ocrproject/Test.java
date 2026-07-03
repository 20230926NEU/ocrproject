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
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class Test extends Application {
    FileChooser fileChooser = new FileChooser();
    File selectedFile = null;
    Label topPaneLabel = null;
    ImageView image = new ImageView();
    TextArea rightPaneText = new TextArea("Test Text");
    Tesseract tesseract = new Tesseract();
    String ocrOutput = null;
    String tempPreprocessImagePath = "/home/kagit/projects/java2/ocrproject/temp_preprocessed.png";
    String modelSelection = null;
    Alert unimplementNotification = new Alert(Alert.AlertType.ERROR);
    
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
        ComboBox<String> modelDropdown = new ComboBox<>();
        modelDropdown.getItems().addAll("Tesseract", "Placeholder Model", "Online API");
        modelDropdown.setValue("Tesseract");
        modelSelection = modelDropdown.getValue();
        Alert selectFilePopup = new Alert(Alert.AlertType.INFORMATION);
        selectFilePopup.setTitle("File Selection");
        selectFilePopup.setHeaderText("You haven't selected a file.");
        selectFilePopup.setContentText("Please select a file, then try again.");
        selectFileButton.setOnAction (e -> {
            selectFile();
        });
        scanButton.setOnAction (e -> {
            if(selectedFile != null){    
                if (modelDropdown.getValue().equals("Tesseract")){
                    try{
                        File ProcessedImage = PreprocessImage();
                        ocrOutput = tesseract.doOCR(ProcessedImage);
                        rightPaneText.setText(ocrOutput);  
                    }
                    catch (TesseractException e1){
                        e1.printStackTrace();
                    }
                }
                else if (modelDropdown.getValue().equals("Placeholder Model")){
                    unimplementNotification.showAndWait();
                } 
                else if (modelDropdown.getValue().equals("Online API")){
                    unimplementNotification.showAndWait();
                }
            }
            else if(selectedFile == null){
                selectFilePopup.showAndWait();
            }
        });
        topPaneButtons.getChildren().addAll(selectFileButton, scanButton, modelDropdown);
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

    public File PreprocessImage() throws TesseractException {
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

    public static void main(String[] args) {
        launch(args);
    }
}