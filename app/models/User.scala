package models

import be.objectify.deadbolt.scala.models.{Role, Permission, Subject}

/**
  * This simple example doesn't support roles or permissions.
  *
  * @author Steve Chaloner (steve@objectify.be)
  */
case class User(userId: String, name: String, avatarUrl: String) extends Subject {
  override def identifier: String = userId

  override def roles: List[_ <: Role] = List.empty

  override def permissions: List[_ <: Permission] = List.empty
}
