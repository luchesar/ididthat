package com.nature.ididthat

import java.util

import javax.naming.Name
import javax.naming.directory.{Attributes, SearchControls}

import org.springframework.ldap.core._
import org.springframework.ldap.filter.AndFilter
import org.springframework.ldap.filter.EqualsFilter

import scala.util.Try
import LdapAuthenticator._
import scala.util.Failure
import scala.util.Success

object LdapAuthenticator {
  val GROUP_OF_UNIQUE_NAMES = "groupOfUniqueNames"
  val UNIQUE_MEMBER = "uniqueMember"
  val OBJECT_CLASS = "objectClass"
  val LDAP_BASE_DN = "admin.ldap.base"
  val LDAP_USER_OU = "admin.ldap.user.ou"
  val LOAD_PENDING = 1
  val LOAD_COMPLETE = 2
  val CN = "cn"
  val UID = "uid"
  val SN = "sn"
  val MAIL = "mail"
  val GIVEN_NAME = "givenName"
  val OU = "ou"
}

class LdapAuthenticator(ldapTemplate: LdapTemplate,
                        config: Config) {

  def authenticate(id: String, pass: String): Try[User] = {
    try ldapTemplate.authenticate(
      buildUserDn(),
      buildUIDFilter(id),
      pass
    ) match {
      case true => Success(findUser(id))
      case false => Failure(new Exception(s"User $id not authenticated. Wrong user name or password"))
    }
    catch {
      case e: Exception => Failure(e)
    }
  }

  private def findUser(id: String): User = {
    ldapTemplate.search(
      buildUserDn(),
      buildUIDFilter(id),
      new AttributesMapper {
        override def mapFromAttributes(attrs: Attributes): Object = {
          //          val uid = attrs.get(UID).get().asInstanceOf[String]
          val name = attrs.get(GIVEN_NAME).get().asInstanceOf[String]
          val surName = attrs.get(SN).get().asInstanceOf[String]
          val fullName = attrs.get(CN).get().asInstanceOf[String]
          val email = attrs.get(MAIL).get().asInstanceOf[String]
          val roles = userRoles(fullName)
          User(id, name, surName, fullName, email, roles)
        }
      }
    ).get(0).asInstanceOf[User]
  }

  private def userRoles(fullName: String): Set[String] = {
    val roleList: util.List[_] = ldapTemplate.search(
      buildUserDn(),
      buildRoleSearchFilter(fullName),
      buildSearchControls(),
      new ContextMapper() {
        private var roles = Set[String]()

        override def mapFromContext(ctx: Object): Set[String] = {
          val context = ctx.asInstanceOf[DirContextAdapter]

          val rolesArray = context.getStringAttributes(CN)
          if (rolesArray != null && rolesArray.length > 0)
            roles += rolesArray(0)
          roles
        }
      }
    )

    if (roleList != null && roleList.size() > 0)
      roleList.get(0).asInstanceOf[Set[String]]
    else Set[String]()
  }

  private def buildRoleSearchFilter(fullName: String): String = {
    val roleFilter = new AndFilter()
    roleFilter.and(new EqualsFilter(OBJECT_CLASS, GROUP_OF_UNIQUE_NAMES))
    roleFilter.and(new EqualsFilter(UNIQUE_MEMBER, buildUniqueMemberDN(fullName)))
    roleFilter.toString
  }

  private def buildSearchControls(): SearchControls = {
    val controls = new SearchControls()
    controls.setSearchScope(SearchControls.ONELEVEL_SCOPE)
    controls.setReturningObjFlag(true)
    controls
  }

  private def buildUserDn(): Name = {
    val dn = new DistinguishedName(config.ldapBase)
    dn.add(OU, config.ldapUserOU)
    dn
  }

  private def buildUIDFilter(userId: String): String = {
    val uidFilter = new AndFilter()
    uidFilter.and(new EqualsFilter(UID, userId))
    uidFilter.toString
  }

  private def buildUniqueMemberDN(fullName: String): String = {
    val dn = new DistinguishedName(config.ldapBase)
    dn.add(OU, config.ldapUserOU)
    dn.add(CN, fullName)
    dn.toString
  }

  implicit def mapper[T](f: Attributes => Object): Object =
    new AttributesMapper {
      override def mapFromAttributes(evt: Attributes): Object = f(evt)
    }
}
