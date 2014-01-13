package codecs

import play.Logger
import play.api.libs.json._

import models._

object JsonApiCodec {	

	def toJson(game: Game, playerId: String) = {

		def stateCode(state: PieceState) = state match {
			case Neutral => 0
			case Taken(player) =>
				if(player.id == playerId) 1
				else 2
		}

		val (playerOne, playerTwo) = game.players
		val me = if(playerId == playerOne.id) playerOne else playerTwo
		val opponent = if(playerId == playerOne.id) playerTwo else playerOne
		val numMoves = game.moves.size
		val isMyTurn = playerId match {
			case playerOne.id => numMoves % 2 == 0
			case playerTwo.id => numMoves % 2 == 1
			case _ => throw new Exception("Unknown player " + playerId)
		}
		val last = if(numMoves == 0) "" else game.moves.head.pieces.map { p => p.display }.mkString
		Json.obj(
			"size"			-> game.board.size,
			"me"			-> me.id,
			"opponent"		-> opponent.id,
			"isMyTurn"		-> isMyTurn,
			"last"			-> last,
			"pieces"		-> game.getStates.map { case (p, s) => 
				Json.obj(
					"display" -> p.display.toString,
					"state" -> stateCode(s)
				)
			}
		)
	}
}
