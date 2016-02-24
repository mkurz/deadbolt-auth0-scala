package controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.ActionBuilders
import security.AuthSupport
import play.api.mvc.Controller
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Steve Chaloner (steve@objectify.be)
  */
class Application @Inject()(actionBuilder: ActionBuilders,
                           authSupport: AuthSupport) extends Controller {

  def index = actionBuilder.SubjectPresentAction().defaultHandler() { authRequest =>
    authSupport.currentUser(authRequest).map(maybeUser =>
      Ok(views.html.index("Protected content", maybeUser)))
  }
}
