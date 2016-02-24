package models

import java.util
import java.util.Collections

import be.objectify.deadbolt.core.models.{Role, Permission, Subject}

/**
  * Note the use of java.util.List.  From 2.5.0 onwards, the API is pure Scala
  * For this simple example, we don't need roles or permissions
  * @author Steve Chaloner (steve@objectify.be)
  */
case class User(userId: String, name: String, avatarUrl: String) extends Subject {
  override def getIdentifier: String = userId

  override def getRoles: util.List[_ <: Role] = Collections.emptyList()

  override def getPermissions: util.List[_ <: Permission] = Collections.emptyList()
}
