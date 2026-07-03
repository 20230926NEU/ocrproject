package com.ocrproject;

import java.io.File;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import nu.pattern.OpenCV;



public class Test extends Application {
    FileChooser fileChooser = new FileChooser();
    File selectedFile = null;
    Label topPaneLabel = null;
    ImageView image = new ImageView();
    TextArea rightPaneText = new TextArea("Test Text");
    Tesseract tesseract = new Tesseract();
    String ocrOutput = null;
    
    @Override
    public void start(Stage stage){
        OpenCV.loadLocally();
        stage.setTitle("Test Application");
        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");

        BorderPane borderPane = new BorderPane();
        
        VBox topPane = getTopPane();
        borderPane.setTop(topPane);

        VBox centerPane = getCenterPane();
        borderPane.setLeft(centerPane);

        VBox rightPane = getRightPane();
        borderPane.setRight(rightPane);

        Scene scene = new Scene(borderPane, 650, 350, Color.WHITESMOKE);

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
        Button button1 = new Button("Select File");        
        Button button2 = new Button("Scan");
        topPaneLabel = new Label("Please select a file.");
        button1.setOnAction (e -> {
            fileChooser.setTitle("Select an Image");
            FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter("Image Files (*.jpg, *.png)", "*.jpg", "*.png");
            fileChooser.getExtensionFilters().add(fileFilter);      
            selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile !=null){
                topPaneLabel.setText("Selected: " + selectedFile.getName());
                Image selectedImage = new Image(selectedFile.toURI().toString());
                image.setImage(selectedImage);
            }
        });
        button2.setOnAction (e -> {
            if(selectedFile != null){    
            try {
                ocrOutput = tesseract.doOCR(selectedFile);
            } 
            catch (TesseractException e1){
                e1.printStackTrace();
            }
            rightPaneText.setText(ocrOutput);         
            }
        });
        topPaneButtons.getChildren().addAll(button1, button2);
        topPane.getChildren().addAll(topPaneButtons, topPaneLabel);
        return topPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}