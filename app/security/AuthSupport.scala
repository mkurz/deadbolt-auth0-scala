package security

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.scala.AuthenticatedRequest
import models.User
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.Session
import play.api.{Configuration}
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Support for user authentication.
  *
  * @author Steve Chaloner (steve@objectify.be)
  */
@Singleton
class AuthSupport @Inject()(cache: CacheApi,
                            config: Configuration,
                            ws: WSClient) {

  private object SessionKeys {
    val IdToken: String = "idToken"
    val AccessToken: String = "accessToken"
  }

  private val clientId: String = config.getString(Auth0ConfigKeys.ClientId).getOrElse("configure me properly")
  private val clientSecret: String = config.getString(Auth0ConfigKeys.ClientSecret).getOrElse("configure me properly")
  private val domain: String = config.getString(Auth0ConfigKeys.Domain).getOrElse("configure me properly")
  private val redirectUri: String = config.getString(Auth0ConfigKeys.RedirectURI).getOrElse("configure me properly")

  /**
    * Get the current user by checking, in order, the request, the cache and the
    * identity management platform.
    *
    * @param request the HTTP request
    * @return a future for an option of the user
    */
  def currentUser[A](request: AuthenticatedRequest[A]): Future[Option[User]] = {
    val maybeIdToken: Option[String] = request.session.get(SessionKeys.IdToken)
    maybeIdToken match {
      case Some(idToken) =>
        val maybeLocalUser: Option[User] = request.subject.map(subject => subject.asInstanceOf[User]).orElse {
          val maybeCached: Option[User] = cache.get(cacheKey(idToken))
          maybeCached match {
            case Some(user) => maybeCached
            case None => Option.empty
          }
        }
        maybeLocalUser match {
          case Some(user) => Future.successful(maybeLocalUser)
          case None => getUser(request.session(SessionKeys.AccessToken))
                       .map(userJson => Option(bindAndCache(idToken,
                                                             userJson)))
        }
      case None => Future.successful(Option.empty)
    }
  }

  /**
    * Namespace the cache key for users.
    *
    * @param id the variable part of the key
    * @return the standardised cache key
    */
  def cacheKey(id: String): String = "user.cache." + id

  /**
    * Using the authentication code from Auth0, get the user's token
    * and a JSON Web Token.
    *
    * @param code the authentication code
    * @return a tuple containing the user token and JWT.
    */
  def getToken(code: String): Future[(String, String)] = {
    val tokenResponse = ws.url(s"https://$domain/oauth/token")
                        .withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
                        .post(Json.obj("client_id" -> clientId,
                                       "client_secret" -> clientSecret,
                                       "redirect_uri" -> redirectUri,
                                       "code" -> code,
                                       "grant_type" -> "authorization_code"))

    tokenResponse.flatMap { response =>
      (for {
        idToken <- (response.json \ "id_token").asOpt[String]
        accessToken <- (response.json \ "access_token").asOpt[String]
      } yield {
        Future.successful((idToken, accessToken))
      }).getOrElse(Future.failed[(String, String)](new IllegalStateException("Tokens not sent")))
    }
  }

  /**
    * Get the user's attributes from Auth0.
    *
    * @param accessToken the user's access token
    * @return a future containing the user's attributes in JSON
    */
  def getUser(accessToken: String): Future[JsValue] = {
    val userResponse = ws.url(s"https://$domain/userinfo")
                       .withQueryString("access_token" -> accessToken)
                       .get()

    userResponse.flatMap(response => Future.successful(response.json))
  }

  /**
    * Create a User instance from the attributes provided by Auth0.  The
    * resulting user is cached for future use.
    *
    * @param idToken the token identifying the user
    * @param userJson the user's attributes in JSON form
    */
  def bindAndCache(idToken: String,
                   userJson: JsValue): User = {
    val user: User = User(userId = (userJson \ "user_id").get.as[String],
                           name = (userJson \ "name").get.as[String],
                           avatarUrl = (userJson \ "picture").get.as[String])
    cache.set(cacheKey(idToken),
               user)
    user
  }

  /**
    * Add authentication info to the session, maintaining existing data.
    *
    * @param session the existing session
    * @param idToken the JWT
    * @param accessToken the user's token
    * @return a session combining existing data and the authentication info
    */
  def authenticatedSession(session: Session,
                           idToken: String,
                           accessToken: String): Session =
    new Session(session.data ++ Map("idToken" -> idToken,
                                     "accessToken" -> accessToken))

  /**
    * Remove the user from the cache, and remove authentication info from
    * the session.  Other session values are maintained.
    *
    * @param session the existing session
    * @return a copy of the existing session minus the authentication info
    */
  def cleanUp(session: Session): Session = {
    cache.remove(cacheKey(session("idToken")))
    new Session(session.data -- Seq("idToken", "accessToken"))
  }
}
