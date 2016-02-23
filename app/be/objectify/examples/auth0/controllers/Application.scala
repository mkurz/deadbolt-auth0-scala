package be.objectify.examples.auth0.controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.ActionBuilders
import be.objectify.examples.auth0.security.AuthSupport
import play.api.mvc.Controller
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Steve Chaloner (steve@objectify.be)
  */
class Application @Inject()(actionBuilder: ActionBuilders,
                           authSupport: AuthSupport) extends Controller {

  def index = actionBuilder.SubjectPresentAction().defaultHandler() { authRequest =>
    authSupport.currentUser(authRequest).map(maybeUser =>
      Ok(be.objectify.examples.auth0.views.html.index("Protected content", maybeUser)))
  }
}
