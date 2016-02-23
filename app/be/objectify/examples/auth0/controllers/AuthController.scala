package be.objectify.examples.auth0.controllers

import javax.inject.Inject

import be.objectify.examples.auth0.models.User
import be.objectify.examples.auth0.security.{AuthSupport, Auth0ConfigKeys}
import play.api.{Configuration, Play}
import play.api.cache.CacheApi
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS
import play.api.mvc.{Session, Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * Based on code taken directly from Auth0.
  */
class AuthController @Inject()(cache: CacheApi,
                               config: Configuration,
                               authSupport: AuthSupport) extends Controller {

  private val clientId: String = config.getString(Auth0ConfigKeys.ClientId).getOrElse("configure me properly")
  private val domain: String = config.getString(Auth0ConfigKeys.Domain).getOrElse("configure me properly")
  private val redirectUri: String = config.getString(Auth0ConfigKeys.RedirectURI).getOrElse("configure me properly")

  // callback route
  def callback(codeOpt: Option[String] = None) = Action.async { request =>
    (for {
      code <- codeOpt
    } yield {
      authSupport.getToken(code).flatMap { case (idToken, accessToken) =>
        authSupport.getUser(accessToken).map { userJson =>
          authSupport.bindAndCache(idToken,
                                    userJson)
          Redirect(routes.Application.index()).withSession(authSupport.authenticatedSession(request.session,
                                                                                             idToken,
                                                                                             accessToken))
        }
      }.recover {
        case ex: IllegalStateException => Unauthorized(ex.getMessage)
      }
    }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
  }

  def logIn = Action.async {
    Future {
      Ok(be.objectify.examples.auth0.views.html.security.login(clientId,
                                                                domain,
                                                                redirectUri))
    }
  }

  def logOut = Action.async { request =>
    Future {
      Ok(be.objectify.examples.auth0.views.html.security.login(clientId,
                                                                domain,
                                                                redirectUri))
      .withSession(authSupport.cleanUp(request.session))
    }
  }
}
