package controllers

import play.Logger
import play.api.mvc.{Controller, Action, WebSocket}
import play.api.libs.json.Json.toJson
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.concurrent.Execution.Implicits._

import utils.{Result, Failure, Success, WordListReader}

object Game extends Controller {

	lazy val words = WordListReader.readWordsFromEdictGzip("public/wordlists/edict2.gz")
	
	def isWord(input: String) = Action {
		if (words.contains(input)) Ok("exists")
		else BadRequest(input + " is not a recognized word")
	}

	def index = Action {
		val game = persistence.Repository.create(4, "one", "two")
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
			val known = words.contains(word)
			Logger.info(s"word is $word (${ids}) (known? $known)")
			if (known) Success(word)
			else Failure("Word not recognized")
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

	def subscribeWS(id: Long, playerId: String) = WebSocket.using[String] { request => 
		val in = Iteratee.consume[String]()
		val obs = persistence.Repository.updatesOn(id)
		val out = Concurrent.unicast[String](channel =>
			obs.subscribe(g => {
				Logger.error(s"SUB WS update: $g")
				channel.push(g.id.toString)
			})
		)
		(in, out)
	}
}