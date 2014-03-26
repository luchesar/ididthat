package com.nature.ididthat

case class User(id: String,
                name: String,
                surName: String,
                fullName: String,
                email: String,
                roles: Set[String])

<<<<<<< HEAD
=======
case class Permission(id: String)
//
//object Permission extends Enumeration {
//  type Permission = Value
//  val ListUsers, ListTasks = Value
//}

//case class Role(id: String, permissions: Set[Permission.Value])
>>>>>>> 4af4e62beba512d08d29babbda0ecd173694c6ab
case class Role(id: String, permissions: Set[String])

case class Task(id: String, date: Long, user: String, work: String)    //TODO: change this date into some Date. We need an implicit converter to JSON

case class Team(id: String, name: String, users: Set[String])
