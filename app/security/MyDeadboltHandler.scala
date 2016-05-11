package security

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DynamicResourceHandler, DeadboltHandler}
import models.User
import views.html.security.{login, denied}
import play.api.Configuration
import play.api.mvc.{Results, Result, Request}
import play.twirl.api.HtmlFormat
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * @author Steve Chaloner (steve@objectify.be)
  */
@Singleton
class MyDeadboltHandler @Inject() (config: Configuration,
                                   authSupport: AuthSupport) extends DeadboltHandler {

  private val clientId: String = config.getString(Auth0ConfigKeys.ClientId).getOrElse("configure me properly")
  private val domain: String = config.getString(Auth0ConfigKeys.Domain).getOrElse("configure me properly")
  private val redirectUri: String = config.getString(Auth0ConfigKeys.RedirectURI).getOrElse("configure me properly")

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future {None}

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future {None}

  /**
    * Get the current user.
    *
    * @param request the HTTP request
    * @return a future for an option maybe containing the subject
    */
  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = authSupport.currentUser(request)

  /**
    * Handle instances of authorization failure.
    *
    * @param request the HTTP request
    * @return either a 401 or 403 response, depending on the situation
    */
  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    def toContent(maybeSubject: Option[Subject]): (Boolean, HtmlFormat.Appendable) =
      maybeSubject.map(subject => subject.asInstanceOf[User])
      .map(user => (true, denied(Some(user))))
      .getOrElse {(false, login(clientId, domain, redirectUri))}

    getSubject(request).map(maybeSubject => toContent(maybeSubject))
    .map(subjectPresentAndContent =>
      if (subjectPresentAndContent._1) Results.Forbidden(subjectPresentAndContent._2)
      else Results.Unauthorized(subjectPresentAndContent._2))
  }
}
