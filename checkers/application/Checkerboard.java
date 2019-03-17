package application;

import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Marek Oleksik
 */
public class Checkerboard {
	private double boardHeight;
	private double boardWidth;
	private double rectangleHeight;
	private double rectangleWidth;
	private final int NUMCOLS = 8;
	private final int NUMROWS = 8;

	private AnchorPane gameboard = null;
	private final Color LIGHTCOLOR = Color.BEIGE;;
	private final Color DARKCOLOR = Color.BURLYWOOD;;

	public Checkerboard(double boardWidth, double boardHeight) {
		this.boardWidth = boardWidth;
		this.boardHeight = boardHeight;
	}

	public AnchorPane build() {
		// Calculate the max width and height of the board squares using the
		// smallest board dimension
		if (boardWidth < boardHeight) {
			rectangleWidth = boardWidth / NUMCOLS;
			rectangleHeight = boardWidth / NUMROWS;
		} else {
			rectangleWidth = boardHeight / NUMCOLS;
			rectangleHeight = boardHeight / NUMROWS;
		}

		gameboard = new AnchorPane();

		// Create board squares
		for (int i = 0; i < NUMROWS; i++) {
			for (int j = 0; j < NUMCOLS; j++) {

				// Create board square
				Rectangle boardSquare = new Rectangle();
				boardSquare.setWidth(rectangleWidth);
				boardSquare.setHeight(rectangleHeight);
				boardSquare.setX(rectangleWidth * j);
				boardSquare.setY(rectangleHeight * i);

				// Assign board square color
				if ((j % 2 == 0 && i % 2 != 0) || (j % 2 != 0 && i % 2 == 0)) {
					boardSquare.setFill(DARKCOLOR);
				} else {
					boardSquare.setFill(LIGHTCOLOR);
				}

				// Add board square to anchor pane
				gameboard.getChildren().add(boardSquare);
			}
		}
		return gameboard;
	}

	public AnchorPane getBoard() {
		return gameboard;
	}

	public double getWidth() {
		return boardWidth;
	}

	public double getHeight() {
		return boardHeight;
	}

	public double getRectangleWidth() {
		return rectangleWidth;
	}

	public double getRectangleHeight() {
		return rectangleHeight;
	}
}