package controllers

import play.Logger
import play.api.mvc.{Controller, Action}
import play.api.libs.json.Json.toJson
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.EventSource

import utils.{Result, Failure, Success, WordListReader}

object Game extends Controller {

	lazy val words = WordListReader.readWordsFromEdictGzip("public/wordlists/edict2.gz")
	lazy val chars = words.toList.flatten
	
	def isWord(input: String) = Action {
		if (words.contains(input)) Ok("exists")
		else BadRequest(input + " is not a recognized word")
	}

	def index = Action {
		import scala.util.Random
		val boardSize = 6

		def charSelector = {
			val r = new Random()
			var pos = r.nextInt(chars.size - (boardSize * boardSize))
			() => {
				pos +=1
				chars(pos)
			}
		}

		val game = persistence.Repository.create(boardSize, "one", "two", charSelector)
		Redirect(routes.Game.board(game.id, "one"))
	}

	def data(id: Long, playerId: String) = Action {
		import codecs.JsonApiCodec
		persistence.Repository.find(id) map { game =>
			Ok(JsonApiCodec.toJson(game, playerId))
		} getOrElse {
			BadRequest(toJson("No game with id " + id))
		}
	}

	def board(id: Long, playerId: String) = Action { implicit request =>
		Ok(views.html.board(id, playerId))
	}

	def move(id: Long, playerId: String) = Action(parse.json) { implicit request =>
		import play.api.libs.json.JsSuccess
		def knownGame = persistence.Repository.find(id) match {
			case Some(g) => Success(g)
			case None => Failure("Unknown game")
		}
		def validInput = request.body.validate[List[Int]] match {
			case JsSuccess(value, path) => Success(value)
			case _ => Failure("Missing pieces to move")
		}
		def knownWord(game: models.Game, ids: List[Int]) = {
			val word = game.getWord(ids)
			if (words.contains(word)) Success(word)
			else Failure(s"'$word' not recognized")
		}
		def isAccepted = for {
			game <- knownGame
			ids <- validInput
			word <- knownWord(game, ids)
		} yield (game, ids)

		isAccepted match {
			case Success((game, ids)) => {
				persistence.Repository.move(game, ids)
				Ok("Move accepted")
			}
			case Failure(msg) => BadRequest(msg)
		}
	}

	def subscribeSSE(id: Long, playerId: String) = Action {
		val updates = persistence.Repository.updatesOn(id)
		val enumerator = Concurrent.unicast[String](channel =>
			updates.subscribe(g => {
				Logger.error(s"SSE update: $g")
				channel.push(g.id.toString)
			})
		)
		Ok.stream(enumerator &> EventSource()).as( "text/event-stream")
	}
}