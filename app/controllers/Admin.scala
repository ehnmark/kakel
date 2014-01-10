package controllers

import play.api.mvc.{Controller, Action}
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, number, mapping}

case class CreateGameRequest(playerIdOne: String, playerIdTwo: String, size: Int)

object Admin extends Controller {

	val moveForm = Form("pieceIds" -> nonEmptyText)
	val createForm = Form(
		mapping(
			"playerIdOne" -> nonEmptyText,
			"playerIdTwo" -> nonEmptyText,
			"size" -> number(min=2, max=7)
		)(CreateGameRequest.apply)(CreateGameRequest.unapply)
	).fill(CreateGameRequest("one", "two", 3))
	
	def delete(id: Long) = TODO

	def index = Action {
		Ok(views.html.admin(persistence.Repository.all.values, createForm, moveForm))
	}

	def create = Action { implicit request => 
		createForm.bindFromRequest.fold(
			errors => BadRequest(views.html.admin(persistence.Repository.all.values, errors, moveForm)),
			model => {
				persistence.Repository.create(model.size, model.playerIdOne, model.playerIdTwo)
				Redirect(routes.Admin.index)
			}
		)
	}

	def move(id: Long) = Action { implicit request =>
		moveForm.bindFromRequest.fold(
			errors => BadRequest(views.html.admin(persistence.Repository.all.values, createForm, errors)),
			pieceIdStr => {
				val pieceIds = pieceIdStr.split(",").map(_.trim.toInt).toList
				val game = persistence.Repository.find(id).get
				persistence.Repository.move(game, pieceIds)
				Redirect(routes.Admin.index)
			}
		)
	}
}