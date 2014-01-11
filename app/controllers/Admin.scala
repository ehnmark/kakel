package controllers

import play.api.mvc.{Controller, Action}
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, number, mapping}

object Admin extends Controller {

	val moveForm = Form("pieceIds" -> nonEmptyText)
	def delete(id: Long) = TODO

	def index = Action {
		Ok(views.html.admin(persistence.Repository.all.values, moveForm))
	}

	def move(id: Long) = Action { implicit request =>
		moveForm.bindFromRequest.fold(
			errors => BadRequest(views.html.admin(persistence.Repository.all.values, errors)),
			pieceIdStr => {
				val pieceIds = pieceIdStr.split(",").map(_.trim.toInt).toList
				val game = persistence.Repository.find(id).get
				persistence.Repository.move(game, pieceIds)
				Redirect(routes.Admin.index)
			}
		)
	}
}