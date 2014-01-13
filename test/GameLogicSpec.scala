import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._

import models._

class GameLogicSpec extends Specification {
	"a game should" should {
		"have zero moves initially" in {
			val board = Board.create(2, () => 'A')
			val players = (new Player("one"), new Player("two"))
			val game = Game(1, players, board, Nil)
			game.moves must have size(0)
		}
	}
}