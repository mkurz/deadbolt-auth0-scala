package security

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}
import be.objectify.deadbolt.scala.cache.HandlerCache

/**
  * @author Steve Chaloner (steve@objectify.be)
  */
@Singleton
class MyHandlerCache @Inject() (defaultHandler: DeadboltHandler) extends HandlerCache {

  private val handlers: Map[String, DeadboltHandler] = Map(defaultHandler.handlerName -> defaultHandler)

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(key: HandlerKey): DeadboltHandler = handlers.get(key.toString).orNull
}
