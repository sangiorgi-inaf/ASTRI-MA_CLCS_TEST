package it.inaf.iasfpa.astri.camera.launcher;


import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class Main extends Application {

	private FXMLLoader loader = new FXMLLoader();



	@Override
	public void start(Stage primaryStage) {
		try {
			AnchorPane root = (AnchorPane) loader.load(getClass().getResource("LauncherGui.fxml").openStream());
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("ASTRI Camera Launcher");

			primaryStage.show();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	public static void main(String[] args) {
		launch(args);
	}
}
