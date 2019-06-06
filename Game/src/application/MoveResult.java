package application;

/**
 * Klasa zawiera zmienne oraz metody odpowiedzialne za obsługę rezultatu wykonanego ruchu pionkiem
 * @author Marek Oleksik
 */

public class MoveResult {
	private MoveType type;

	public MoveType getType() {
		return type;
	}

	private Piece piece;

	public Piece getPiece() {
		return piece;
	}

	public MoveResult(MoveType type) {
		this(type, null);
	}

	public MoveResult(MoveType type, Piece piece) {
		this.type = type;
		this.piece = piece;
	}
}
