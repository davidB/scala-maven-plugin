package com.liftone.model

import net.liftweb.mapper._
import net.liftweb.util._

/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MetaMegaProtoUser[User, User with KeyedMetaMapper[Long, User]] {
  override def dbTableName = "users" // define the DB table name
  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)
  // define the order fields will appear in forms and output
  override def fieldOrder = id :: firstName :: lastName :: email :: 
  locale :: timezone ::
  password :: textArea :: Nil

  // comment this line out to require email validations
  override def skipEmailValidation = true
}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] {
  def getSingleton = User // what's the "meta" server
  def primaryKeyField = id
  
  // define an additional field for a personal essay
  object textArea extends MappedTextarea(this, 2048) {
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = "Personal Essay"
  }
}
