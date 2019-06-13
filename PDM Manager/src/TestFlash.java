

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;

public class TestFlash extends Application {
	
		
	@Override
	public void start(Stage primaryStage) {
		try {			
			AnchorPane root = (AnchorPane)FXMLLoader.load(getClass().getResource("it/inaf/iasfpa/astri/camera/pdm/gui/PDMFlasher.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("it/inaf/iasfpa/astri/camera/pdm/gui/application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("PDM Flasher");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	} 
	
}
