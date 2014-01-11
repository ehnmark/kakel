package models

sealed trait PieceState
case class Taken(player: Player) extends PieceState {
	override def toString = "T(%s)".format(player)
}
object Neutral extends PieceState {
	override def toString = "N"
}

case class Standings(players: (Player, Player), scores: (Int, Int))
case class GameState(isGameOver: Boolean, standings: Standings, states: Map[Piece, PieceState]) {
	override def toString = {
		val (playerOne, playerTwo) = standings.players
		val (scoreOne, scoreTwo) = standings.scores
		val prefix = if(isGameOver) "over" else  "ongoing"
		"%s (%s %d, %s %d)".format(prefix, playerOne, scoreOne, playerTwo, scoreTwo)
	}
}

case class Player(id: String) {
	override def toString = id.toString
}

case class Piece(display: Char, x: Int, y: Int) {
	override def toString = display.toString
}

case class Move(pieces: List[Piece]) {
	override def toString = pieces.mkString
}

case class Board(size: Int, pieces: Map[Int, Piece]) {

	override def toString = pieces
		.toList
		.sortWith { case ((idx1, piece1), (idx2, piece2)) => idx1 <= idx2 }
		.map { case (idx, piece) => 
			piece.toString + (if ((1 + idx) % size == 0) "\n" else "") }.mkString
}

case class Game(id: Long, players: (Player, Player), board: Board, moves: List[Move]) {

	def getPieces(pieceIds: List[Int]) = {
		pieceIds map { id => board.pieces(id) }
	}

	def getWord(pieceIds: List[Int]) = {
		getPieces(pieceIds) map { p => p.display } mkString
	}

	def getStates = {
		val initial: Map[Piece, PieceState] = board.pieces map { case (idx, piece) => piece -> Neutral }
		moves.reverse.zipWithIndex.foldLeft(initial) { case (acc, (move, index)) => {
			val player = if (index % 2 == 0) players._1 else players._2
			acc ++ move.pieces.map {
				piece => piece -> new Taken(player)
			}
		}}
	}

	def getGameState = {
		val states = getStates
		val (playerOne, playerTwo) = players
		val isOver = ! (states exists { case (p, s) => s == Neutral })
		def playerScore(player: Player) = {
			val key = Taken(player)
			states map { case (p, s) => if(s == key) 1 else 0 } sum
		}
		val standings = Standings(players, (playerScore(playerOne), playerScore(playerTwo)))
		GameState(isOver, standings, states)
	}

	def move(pieces: List[Piece]) = {
		Game(id, players, board, new Move(pieces) :: moves)
	}

	override def toString = {
		val (playerOne, playerTwo) = players
		"%d: %s x %s".format(id, playerOne, playerTwo)
	}
}

object Board {

	def create(size: Int, charSelector: () => Char) = {
		new Board(size, (for {
			y <- 0 until size
			x <- 0 until size
		} yield (x + y * size) -> new Piece(charSelector(), x, y)).toMap)
	}
}
