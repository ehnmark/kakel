package persistence

import play.Logger
import play.api.libs.json._

import models._

object JsonCodec {

	def fromJson(input: String): Game = fromJson(Json.parse(input))

	def fromJson(input: JsValue) = {
		val id = (input \ "id").as[Long]
		val playerOneId = (input \ "player_one").as[String]
		val playerTwoId = (input \ "player_two").as[String]
		val size = (input \ "size").as[Int]

		val pieces = (input \ "pieces").as[JsArray].value.map { jsval => 
			val x = (jsval \ "x").as[Int]
			val y = (jsval \ "y").as[Int]
			val display = (jsval \ "display").as[String].head
			new Piece(display, x, y)
		}
		val pieceMap = pieces.zipWithIndex.map { case (p, i) => (i, p) }.toMap
		val moves = (input \ "moves").as[JsArray].value.map { jsval =>
			val ids = jsval.as[List[Int]]
			val pieces = ids map { id => pieceMap(id) }
			new Move(pieces)
		}.reverse.toList
		val board = new Board(size, pieceMap)
		val players = (new Player(playerOneId), new Player(playerTwoId))

		new Game(id, players, board, moves)
	}

	def toJson(game: Game) = {
		val pieceLookup = game.board.pieces.map { case (i, p) => (p, i) }.toMap
		val (playerOne, playerTwo) = game.players
		Json.obj(
			"id" 			-> game.id,
			"player_one"	-> playerOne.id,
			"player_two"	-> playerTwo.id,
			"size"			-> game.board.size,
			"moves"			-> game.moves.reverse.map { move =>
				move.pieces.map { piece => pieceLookup(piece) }
			},
			"pieces"		-> game.getStates.map { case (p, s) => 
				Json.obj(
					"display" -> p.display.toString,
					"x" -> p.x,
					"y" -> p.y,
					"state" -> s.toString
				)
			}
		)
	}

	def toJsonString(game: Game) = Json.stringify(toJson(game))

	implicit object Format extends Format[Game] {
		def reads(input: JsValue) = JsSuccess(fromJson(input))
		def writes(game: Game) = toJson(game)
	}
}
