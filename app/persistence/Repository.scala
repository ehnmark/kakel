package persistence

import anorm._ 
import play.api.db._
import play.api.Play.current

import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject 

import models._

object Repository {

	private lazy val updates = PublishSubject[Game]()

	def updatesOn(id: Long) = updates.filter { g => g.id == id }

	def all = {
		DB.withConnection { implicit conn =>
			SQL("""select json from game""")().map { row =>
				val json = row[String]("json")
				JsonCodec.fromJson(json)
			} map { game => (game.id, game) } toMap
		}
	}

	def find(id: Long) = all.get(id)

	def move(game: Game, pieceIds: List[Int]) = {
		val pieces = game.getPieces(pieceIds)
		val moved = game.move(pieces)
		updates.onNext(game)
		persistUpdate(moved)				
	}

	def create(size: Int, playerOneId: String, playerTwoId: String, charSelector: () => Char) = {
		def getGameId = DB.withConnection { implicit conn => getNextVal("game_seq") }
		val players = (new Player(playerOneId), new Player(playerTwoId))
		val game = new Game(getGameId, players, Board.create(size, charSelector), Nil)
		persistNew(game)
		game
	}

	private def getNextVal(seqName: String)(implicit conn: java.sql.Connection) = {
		SQL("select %s.nextval as id".format(seqName))().map { row => row[Long]("id")}.head
	}

	private def persistNew(game: Game) = {
		val (playerOne, playerTwo) = game.players
		DB.withTransaction { implicit conn =>
			SQL(
			"""
				insert into game (id, json)
				values ({id}, {json})
			""").on(
					"id"	-> game.id,
					"json"	-> JsonCodec.toJsonString(game)
			).execute()
		}
	}

	private def persistUpdate(game: Game) = {
		val (playerOne, playerTwo) = game.players
		DB.withTransaction { implicit conn =>
			SQL(
			"""
				update game set json = {json} where id = {id}
			""").on(
					"id"	-> game.id,
					"json"	-> JsonCodec.toJsonString(game)
			).execute()
		}
	}
}

