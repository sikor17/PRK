package Controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import application.AlertBox;
import application.CheckersBoard;
import application.Player;
import application.Sender;
import application.User;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author Krzysztof Jagodziński
 */
public class GameController {
	@FXML
	private TextField messageTextField;
	@FXML
	private Label welcomeLabel;
	@FXML
	private WebView webViewMessages;
	@FXML
	private Circle circleImage;
	@FXML
	private ImageView sendImageView;
	@FXML
	private ListView<String> userListView;
	@FXML
	private Circle playerWhiteImage;
	@FXML
	private Circle playerRedImage;
	@FXML
	private CheckBox playerWhiteCheckBox;
	@FXML
	private CheckBox playerRedCheckBox;
	@FXML
	private Button aboutButton;
	@FXML
	private Tab tabChat;
	@FXML
	private Tab tabUsers;
	@FXML
	private AnchorPane anchorPane;

	private double boardHeight = 600;
	private double boardWidth = 600;

	private String host;
	// adres serwera
	private final int PORT = 9001;
	// numer portu
	private Socket socket;
	// obiekt gniazda
	private BufferedReader inputBufferedReader;
	// bufor wejściowy (dane odebrane z serwera)
	public static PrintWriter outputPrintWriter;
	// bufor wyjściowy (dane do wysłania)
	private ArrayList<String> usersList = new ArrayList<String>();
	Document messagesLayout;

	// Generate new gameboard
	private CheckersBoard checkerboard = new CheckersBoard(boardWidth, boardHeight);
	private Player player = new Player();
	private Sender sender = new Sender();
	private User user = new User();
	String PMSG_Recipient;
	int PMSG_RecipientID;

	@FXML
	private void initialize() {

		Image myImage = new Image(new File("res/avatars/bot.png").toURI().toString());
		ImagePattern pattern = new ImagePattern(myImage);
		playerWhiteImage.setFill(pattern);
		playerRedImage.setFill(pattern);
		player.setWhitePlayer(""); // Aby nie buło
									// java.lang.NullPointerException
		player.setRedPlayer(""); // Aby nie buło java.lang.NullPointerException
		player.setRedTurn(false);
		String welcome = "Witaj w grze. Wybierz wolne miejsce.";
		messagesLayout = Jsoup.parse("<html><head><meta charset='UTF-8'>"
				+ "</head><body><ul><li class=\"welcome\"><div class=\"message\"><div class=\"content\">" + welcome
				+ "</div></div></li></ul></body></html>", "UTF-16", Parser.xmlParser());
		webViewMessages.getEngine().loadContent(messagesLayout.html());
		webViewMessages.getEngine()
				.setUserStyleSheetLocation(getClass().getResource("/application/application.css").toString());
	}

	public void setUserName(String userName) {
		user.setUserName(userName);
		welcomeLabel.setText("Witaj " + userName + "!");
	}

	public void setPicID(String picID) {
		user.setPicID(picID);
		Image myImage = new Image(new File("res/avatars/" + picID).toURI().toString());
		ImagePattern pattern = new ImagePattern(myImage);
		circleImage.setFill(pattern);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void run() throws UnknownHostException, IOException {
		socket = new Socket(host, PORT);
		inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws IOException {
				while (true) {
					String msg = inputBufferedReader.readLine();
					System.out.println(msg);
					if (msg.startsWith("RDY")) {
						outputPrintWriter.println(user.getUserName());
						outputPrintWriter.println(user.getPicID());
					} else if (msg.startsWith("UID")) {
						user.setUID(Integer.parseInt(msg.substring(3)));
					} else if (msg.startsWith("MSG")) {
						addMessage(toHTML(decodeUID(msg)));
					} else if (msg.startsWith("PMSG")) {
						addMessage(toHTML_PMSG(decodeUID_PMSG(msg)));
					} else if (msg.startsWith("[")) {
						updateUserList(msg);
					} else if (msg.startsWith("SIT")) {
						updateUserSit(msg);
					} else if (msg.startsWith("STAND")) {
						updateUserStand(msg);
					} else if (msg.startsWith("MOVE")) {
						setActiveUser(msg);
					} else if (msg.startsWith("NEXT")) {
						updateActiveUser();
					} else if (msg.startsWith("BRD")) {
						setBoardString(msg);
					} else if (msg.startsWith("QUIT")) {
						quitResultMessage(msg);
					}
				}
			}
		};
		new Thread(task).start();
	}

	public void closeSocket() throws IOException {
		this.socket.close();

	}

	private void setBoardString(String msg) {
		// CheckersBoard.boardString.clear();
		msg = msg.substring(3);
		String[] board = msg.split(",");
		int i = 0;
		for (int col = 0; col < 8; col++) {
			for (int row = 0; row < 8; row++) {

				CheckersBoard.boardString[row][col] = board[i];
				i++;

			}
		}
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				boardWidth = 600;
				boardHeight = 600;

				// Generate new gameboard
				CheckersBoard checkerboard = new CheckersBoard(boardWidth, boardHeight);
				AnchorPane gameboard = checkerboard.fill();
				anchorPane.getChildren().clear();
				anchorPane.getChildren().addAll(gameboard);
			}
		});
	}

	private void quitResultMessage(String msg) {
		msg = msg.substring(5);
		String[] param = msg.split("\t");
		// Aby nie wywoływać Not on FX
		// application thread;
		// currentThread=Thread-5
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (param[0].equals("WHITE")) {
					if (param[1].equals(String.valueOf(user.getUID()))) {
						if (AlertBox.showAndWait(AlertType.INFORMATION, "WARCABY", "Rezygacja - PRZEGRYWASZ!!!")
								.orElse(ButtonType.CANCEL) == ButtonType.OK) {
						}
					} else if (player.getRedPlayer().equals(user.getUserName() + user.getUID())) {
						if (AlertBox
								.showAndWait(AlertType.INFORMATION, "WARCABY", "Gracz biały zrezygnował - WYGRYWASZ!!!")
								.orElse(ButtonType.CANCEL) == ButtonType.OK) {
						}
					} else if (AlertBox
							.showAndWait(AlertType.INFORMATION, "WARCABY", "Gracz biały zrezygnował - WYGRYWA CZERWONY!!!")
							.orElse(ButtonType.CANCEL) == ButtonType.OK) {
					}

				}
				
				if (param[0].equals("RED")) {
					if (param[1].equals(String.valueOf(user.getUID()))) {
						if (AlertBox.showAndWait(AlertType.INFORMATION, "WARCABY", "Rezygacja - PRZEGRYWASZ!!!")
								.orElse(ButtonType.CANCEL) == ButtonType.OK) {
						}
					} else if (player.getWhitePlayer().equals(user.getUserName() + user.getUID())) {
						if (AlertBox
								.showAndWait(AlertType.INFORMATION, "WARCABY", "Gracz czerwony zrezygnował - WYGRYWASZ!!!")
								.orElse(ButtonType.CANCEL) == ButtonType.OK) {
						}
					} else if (AlertBox
							.showAndWait(AlertType.INFORMATION, "WARCABY", "Gracz czerwony zrezygnował - WYGRYWA BIAŁY!!!")
							.orElse(ButtonType.CANCEL) == ButtonType.OK) {
					}

				}

			}
		});
	}

	private void updateActiveUser() {

		if (player.isRedTurn()) {
			setActiveUser("MOVE_WHITE");
			System.out.println("MOVE_WHITE");
			player.setRedTurn(false);
		} else {
			setActiveUser("MOVE_RED");
			player.setRedTurn(true);
			System.out.println("MOVE_RED");
		}

	}

	private void setActiveUser(String msg) {
		msg = msg.substring(5);
		if ((msg.equals("RED") && player.getRedPlayer().equals(user.getUserName() + user.getUID()))) {
			player.setRedTurn(true);
			anchorPane.setDisable(false);
		} else if ((msg.equals("WHITE") && player.getWhitePlayer().equals(user.getUserName() + user.getUID()))) {
			player.setRedTurn(false);
			anchorPane.setDisable(false);
		} else {
			anchorPane.setDisable(true);
		}

	}

	private void updateUserList(String msg) {
		usersList.clear();
		msg = msg.substring(1);
		msg = msg.substring(0, msg.length() - 1);
		String[] user = msg.split(", ");

		// Aby nie wywoływać Not on FX
		// application thread;
		// currentThread=Thread-5
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				userListView.getItems().clear();
				for (int i = 0; i < user.length; i++) {
					usersList.add(user[i]);
					String[] param = user[i].split("\t");
					userListView.getItems().add(param[1]);
				}
			}
		});

	}

	private void updateUserSit(String msg) {
		msg = msg.substring(3);
		String[] param = msg.split("\t");

		if (param[3].equals("SIT_WHITE") && !param[0].equals(String.valueOf(user.getUID()))) {

			Platform.runLater(new Runnable() { // Aby nie wywoływać Not on FX
												// application thread;
												// currentThread=Thread-5
				@Override
				public void run() {
					playerWhiteCheckBox.setText(param[1]);
					playerWhiteCheckBox.setDisable(true);
				}
			});
			Image myImage = new Image(new File("res/avatars/" + param[2]).toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerWhiteImage.setFill(pattern);
		}
		if (param[3].equals("SIT_RED") && !param[0].equals(String.valueOf(user.getUID()))) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					playerRedCheckBox.setText(param[1]);
					playerRedCheckBox.setDisable(true);
				}
			});

			Image myImage = new Image(new File("res/avatars/" + param[2]).toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerRedImage.setFill(pattern);
		}

	}

	private void updateUserStand(String msg) {
		msg = msg.substring(5);
		String[] param = msg.split("\t");

		if (param[3].equals("STAND_WHITE") && !param[0].equals(String.valueOf(user.getUID()))) {

			Platform.runLater(new Runnable() { // Aby nie wywoływać Not on FX
												// application thread;
												// currentThread=Thread-5
				@Override
				public void run() {
					playerWhiteCheckBox.setText("Gracz biały");
					// Czy gracz nie drugim miejscu
					if (!playerRedCheckBox.getText().equals(user.getUserName())) {
						playerWhiteCheckBox.setDisable(false);
					}
				}
			});
			Image myImage = new Image(new File("res/avatars/bot.png").toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerWhiteImage.setFill(pattern);
		}
		if (param[3].equals("STAND_RED") && !param[0].equals(String.valueOf(user.getUID()))) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					playerRedCheckBox.setText("Gracz czerwony");
					// Czy gracz nie drugim miejscu
					if (!playerWhiteCheckBox.getText().equals(user.getUserName())) { // Czy
						playerRedCheckBox.setDisable(false);
					}
				}
			});

			Image myImage = new Image(new File("res/avatars/bot.png").toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerRedImage.setFill(pattern);
		}

	}

	@FXML
	private void sendImageView_MouseReleased() {
		if (messageTextField.getLength() == 0)
			return;
		if (tabUsers.isSelected()) { // Wiadomość prywatna
			usersList.get(userListView.getSelectionModel().getSelectedIndex());
			outputPrintWriter.println("PMSG" + usersList.get(userListView.getSelectionModel().getSelectedIndex()) + "\t"
					+ messageTextField.getText());
			messageTextField.clear();
		}
		if (tabChat.isSelected()) { // Wiadomość
			outputPrintWriter.println("MSG" + messageTextField.getText());
			messageTextField.clear();
		}
	}

	@FXML
	private void messageTextField_KeyPressed(KeyEvent e) {
		if (e.getCode() == KeyCode.ENTER) {
			sendImageView_MouseReleased();
		}
	}

	private void addMessage(Element message) {
		Element wrapper = messagesLayout.getElementsByTag("ul").first();
		wrapper.appendChild(message);
		Platform.runLater(new Runnable() {
			public void run() {
				webViewMessages.getEngine().loadContent(messagesLayout.html());
			}
		});
	}

	private Element toHTML(String message) {
		System.out.println("toHTML:" + message);
		String msgClass = (user.getUID() == sender.getSenderUID()) ? "request" : "response";
		Element wrapper = new Element("li").attr("class", msgClass);
		Element image = new Element("img").attr("class", "avatar").attr("src",
				new File("res/avatars/" + user.getPicID()).toURI().toString());
		if (user.getUID() != sender.getSenderUID()) {
			image.attr("src", new File("res/avatars/" + sender.getSenderPicID()).toURI().toString());
			new Element("span").attr("class", "author").append(sender.getSenderName()).appendTo(wrapper);
		}
		image.appendTo(wrapper);
		Element message_div = new Element("div").attr("class", "message").appendTo(wrapper);
		new Element("div").attr("class", "content").append(message).appendTo(message_div);
		return wrapper;
	}

	private String decodeUID(String msg) {
		msg = msg.substring(3);
		String[] param = msg.split("\t");
		sender.setSenderUID(Integer.parseInt(param[0]));
		sender.setSenderName(param[1]);
		sender.setSenderPicID(param[2]);
		return msg.substring(param[0].length() + param[1].length() + param[2].length() + 3);
	}

	private Element toHTML_PMSG(String message) {
		System.out.println("toHTML_PMSG:" + message);
		String msgClass = (user.getUID() == sender.getSenderUID()) ? "request" : "response";
		Element wrapper = new Element("li").attr("class", msgClass);
		if (user.getUID() == sender.getSenderUID() || user.getUID() == PMSG_RecipientID) {
			Element image = new Element("img").attr("class", "avatar").attr("src",
					new File("res/avatars/" + user.getPicID()).toURI().toString());
			if (user.getUID() != sender.getSenderUID() && user.getUID() == PMSG_RecipientID) {
				image.attr("src", new File("res/avatars/" + sender.getSenderPicID()).toURI().toString());
				new Element("span").attr("class", "author").append(sender.getSenderName()).appendTo(wrapper);
			}
			image.appendTo(wrapper);
			Element message_div = new Element("div").attr("class", "message").appendTo(wrapper);
			new Element("div").attr("class", "content").append(message).appendTo(message_div);

		}
		return wrapper;
	}

	private String decodeUID_PMSG(String msg) {
		msg = msg.substring(4);
		String[] param = msg.split("\t");

		sender.setSenderUID(Integer.parseInt(param[0]));
		sender.setSenderName(param[1]);
		sender.setSenderPicID(param[2]);
		PMSG_RecipientID = Integer.valueOf((param[3]));
		PMSG_Recipient = (param[4]);
		return "PRIV -> " + PMSG_Recipient + ": " + (param[5]);
	}

	@FXML
	void playerWhiteCheckBox_OnActrion(ActionEvent event) {
		if (playerWhiteCheckBox.isSelected()) {
			playerWhiteCheckBox.setText(user.getUserName());
			playerRedCheckBox.setDisable(true);
			outputPrintWriter.println("SIT_WHITE");
			player.setWhitePlayer(user.getUserName() + user.getUID());

			Image myImage = new Image(new File("res/avatars/" + user.getPicID()).toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerWhiteImage.setFill(pattern);
			// Czy drugie miejsce jest zajęte
			if (!playerRedCheckBox.getText().equals("Gracz czerwony")) {
				newGame();
			}
		} else {
			playerWhiteCheckBox.setText("Gracz biały");
			// Czy drugie miejsce jest puste
			if (playerRedCheckBox.getText().equals("Gracz czerwony")) {
				playerRedCheckBox.setDisable(false);
			}
			player.setWhitePlayer("");
			outputPrintWriter.println("STAND_WHITE");
			Image myImage = new Image(new File("res/avatars/bot.png").toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerWhiteImage.setFill(pattern);
			// Czy drugie miejsce jest zajęte
			if (!playerRedCheckBox.getText().equals("Gracz czerwony")) {
				outputPrintWriter.println("QUIT_WHITE");
			}
		}

	}

	private void newGame() {
		setGameBoard(); // nowa gra
		outputPrintWriter.println("MOVE_WHITE");
		checkerboard.setBoardServer();
		outputPrintWriter.println("BRD" + convertBoardStringToString());
	}

	@FXML
	void playerRedCheckBox_OnActrion(ActionEvent event) {
		if (playerRedCheckBox.isSelected()) {
			playerRedCheckBox.setText(user.getUserName());
			playerWhiteCheckBox.setDisable(true);
			player.setRedPlayer(user.getUserName() + user.getUID());
			outputPrintWriter.println("SIT_RED");
			Image myImage = new Image(new File("res/avatars/" + user.getPicID()).toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerRedImage.setFill(pattern);
			// Czy drugie miejsce jest zajęte
			if (!playerWhiteCheckBox.getText().equals("Gracz biały")) {
				newGame();
			}
		} else {
			playerRedCheckBox.setText("Gracz czerwony");
			// Czy drugie miejsce jest puste
			if (playerWhiteCheckBox.getText().equals("Gracz biały")) {
				playerWhiteCheckBox.setDisable(false);
			}
			player.setRedPlayer("");
			outputPrintWriter.println("STAND_RED");
			Image myImage = new Image(new File("res/avatars/bot.png").toURI().toString());
			ImagePattern pattern = new ImagePattern(myImage);
			playerRedImage.setFill(pattern);
			// Czy drugie miejsce jest zajęte
			if (!playerWhiteCheckBox.getText().equals("Gracz biały")) {
				outputPrintWriter.println("QUIT_RED");
			}
		}
	}

	@FXML
	void aboutButton_Click(ActionEvent event) {
		try {
			application.ViewLoader<AnchorPane, AboutController> viewLoader = new application.ViewLoader<>("/FXML_Files/About.fxml");
			AnchorPane anchorPaneAbout = viewLoader.getLayout();
			Stage stage = new Stage();
			Scene scene = new Scene(anchorPaneAbout);
			stage.setScene(scene);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("O Programie");
			stage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String convertBoardStringToString() {
		String temp = "";
		for (int col = 0; col < 8; col++) {
			for (int row = 0; row < 8; row++) {
				temp = temp + CheckersBoard.boardString[row][col] + ",";
			}
		}
		temp = temp + "END";
		return temp;
	}

	public void ready(Scene scene) {
		// Create change listener for width/height
		ChangeListener<Number> sizeChangeListener = (ObservableValue<? extends Number> observable, Number oldValue,
				final Number newValue) -> {
			setGameBoard();
		};

		// Add change listeners to scene
		scene.widthProperty().addListener(sizeChangeListener);
		scene.heightProperty().addListener(sizeChangeListener);

	}

	public void setGameBoard() {

		AnchorPane gameboard = checkerboard.build();

		// Clear previous gameboard
		anchorPane.getChildren().clear();

		// Set new gameboard configuration
		anchorPane.getChildren().addAll(gameboard);

	}

}
