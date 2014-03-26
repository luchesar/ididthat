package com.nature.ididthat.integrationtest

import com.nature.ididthat.{LdapAuthenticator, ConfigModule, ConfigTest}
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfter, FunSuite}

class LdapAuthenticatorITest extends FunSuite with BeforeAndAfter {
  val ldapServerPort = 23413
  val config = ConfigTest.mockConfig.copy(
    ldapServerUrl = s"ldap://localhost:$ldapServerPort",
    ldapBase = "dc=someu,dc=edu",
    ldapAdminUserDn = "cn=Smith\\, John,ou=Aerospace Engineering,ou=accounts,dc=someu,dc=edu",
    ldapAdminPass = "password",
    ldapUserOU = "Aerospace Engineering"
  )
  val ldap = new InMemoryLdapServer(ldapServerPort, config.ldapBase, config.ldapAdminUserDn, config.ldapAdminPass, "users.ldif")
  ldap.start()

  val configModule = new ConfigModule(config)

  test("simple") {
    val auth = new LdapAuthenticator(configModule.ldapTemplate, config)
    val isAuth = auth.authenticate("JWilliams1", "password")
    println("working:" + isAuth)
  }

}
