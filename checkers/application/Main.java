package application;

import Controllers.FXMLCheckersBoardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Marek Oleksik */
public class Main extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/FXML_Files/FXMLCheckersBoard.fxml"));
		Parent root = loader.load();

		FXMLCheckersBoardController controller = loader.getController();

		Scene scene = new Scene(root);

		stage.setScene(scene);
		stage.show();

		// Initialize game board
		controller.ready(scene);
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}