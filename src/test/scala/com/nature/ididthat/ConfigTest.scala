package com.nature.ididthat

import org.scalatest.FunSuite
import ConfigTest._
import org.fest.assertions.Assertions._
import java.io.File
import org.apache.commons.io.FileUtils

object ConfigTest {
  val mockConfig = Config(
    port = Config.DefaultPort,
    ldapTimeout = Config.DefaultLdapTimeout,
    ldapServerUrl = "fake-ldap-url",
    ldapAdminUserDn = "uid=pcadmin-admin,ou=administrators,ou=topologymanagement,o=netscaperoot",
    ldapAdminPass = "fakePass",
    ldapBase = "dc=nature,dc=com",
    ldapUserOU = "fakeLdapUserOU",
    mongoHost = "localhost",
    mongoPort = 12345,
    mongoUser = "testMongoUser",
    mongoPass = "testMongoPass",
    mongoDatabase = "testMongoDatabase"
  )
  val mockConfig1 = Config(      //TODO:use to run more specific tests on mongo config
    port = 6543,
    ldapTimeout = 4321,
    ldapServerUrl = "fake-ldap-url1",
    ldapAdminUserDn = "uid=pcadmin-admin,ou=administrators,ou=topologymanagement,o=netscaperoot1",
    ldapAdminPass = "fakePass1",
    ldapBase = "dc=nature,dc=com1",
    ldapUserOU = "fakeLdapUserOU1",
    mongoHost = "localhost",
    mongoPort = 12345,
    mongoUser = "testMongoUser",
    mongoPass = "testMongoPass",
    mongoDatabase = "testMongoDatabase"
  )
}

class ConfigTest extends FunSuite {
  test("ConfigModule should bind everything") {
    val module = new ConfigModule(mockConfig)
    val c1 = module.configuration
    assertThat(c1).isSameAs(mockConfig)
    val c2 = module.configuration
    assertThat(c2).isSameAs(mockConfig)

    val cs1 = module.ldapContextSource
    val cs2 = module.ldapContextSource
    assertThat(cs1).isSameAs(cs2)
    //    assertThat(cs1.getUrls).containsOnly(mockConfig.ldapServerUrl)
    println(cs1.getAuthenticationSource)
    assertThat(cs1.getAuthenticationSource.getPrincipal).isEqualTo(mockConfig.ldapAdminUserDn)
    assertThat(cs1.getAuthenticationSource.getCredentials).isEqualTo(mockConfig.ldapAdminPass)
    assertThat(cs1.isPooled).isFalse()

    val lt1 = module.ldapTemplate
    val lt2 = module.ldapTemplate
    assertThat(lt1).isSameAs(lt2)
    assertThat(lt1.getContextSource).isEqualTo(cs1)

    val mongoConfig1 = module.mongoConfiguration
    val mongoConfig2 = module.mongoConfiguration
    assertThat(mongoConfig1).isSameAs(mongoConfig2)

    assertThat(mongoConfig1.getServers).containsExactly("localhost:12345")
    assertThat(mongoConfig1.getCredentials).hasSize(1)
    val cred = mongoConfig1.getCredentials.iterator().next()
    assertThat(cred.getUserName).isEqualTo("testMongoUser")
    assertThat(cred.getPassword).isEqualTo("testMongoPass".toCharArray)
    assertThat(cred.getDatabase).isEqualTo("testMongoDatabase")
  }

  test("Load config from properties file") {
    withProperties(Some(mockConfig), None) {
      cfg => assertThat(cfg).isEqualTo(mockConfig)
    }
    withProperties(Some(mockConfig1), None) {
      cfg => assertThat(cfg).isEqualTo(mockConfig1)
    }
  }

  test("Load config from cmd arguments") {
    withProperties(None, Some(mockConfig)) {
      cfg => assertThat(cfg).isEqualTo(mockConfig)
    }
    withProperties(None, Some(mockConfig1)) {
      cfg => assertThat(cfg).isEqualTo(mockConfig1)
    }
  }

  test("Override properties file with cmd config") {
    withProperties(Some(mockConfig), Some(mockConfig1)) {
      cfg => assertThat(cfg).isEqualTo(mockConfig1)
    }
  }

  test("Override properties file with cmd config with equals") {
    withProperties(Some(mockConfig), Some(mockConfig1), toArgs1) {
      cfg => assertThat(cfg).isEqualTo(mockConfig1)
    }
  }

  test("Default values") {
    withProperties( s"""
        |ldap-server-url=url
        |ldap-admin-user-dn=user-dn
        |ldap-admin-user-password=pass
        |ldap-base=base
        |ldap-user-ou=user-ou
        |mongo-host=localhost
        |mongo-port=12345
        |mongo-user=test
        |mongo-pass=pass
        |mongo-database=ididthat
         """.stripMargin, Array[String]()) {
      config =>
        assertThat(config.port).isEqualTo(Config.DefaultPort)
        assertThat(config.ldapTimeout).isEqualTo(Config.DefaultLdapTimeout)
    }
    withProperties( s"""
        |port=sdf
        |ldap-timeout=werweru
        |ldap-server-url=url
        |ldap-admin-user-dn=user-dn
        |ldap-admin-user-password=pass
        |ldap-base=base
        |ldap-user-ou=user-ou
        |mongo-host=localhost
        |mongo-port=12345
        |mongo-user=test
        |mongo-pass=pass
        |mongo-database=ididthat
         """.stripMargin, Array[String]()) {
      config =>
        assertThat(config.port).isEqualTo(Config.DefaultPort)
        assertThat(config.ldapTimeout).isEqualTo(Config.DefaultLdapTimeout)
    }
  }

  test("Mandatory values") {
    def filterLines(str: String, containing: String): String = {
      str.split("\n").filter(_.contains(containing)).mkString("\n")
    }
    def assertMandatoryProp(prop: String): Unit =
      try {
        val propsStr = filterLines(toPropStr(Some(mockConfig)), prop)
        withProperties(propsStr, Array[String]())(config => ())
        fail("Illegal Argument Exception expected")
      } catch {
        case e: IllegalArgumentException => assertThat(e.getMessage).contains("prop")
      }

    assertMandatoryProp("ldap-admin-user-dn")
    assertMandatoryProp("ldap-server-url")
    assertMandatoryProp("ldap-admin-user-password")
    assertMandatoryProp("ldap-base")
    assertMandatoryProp("ldap-user-ou")
  }

  private def withProperties(props: Option[Config],
                             args: Option[Config],
                             toA: Option[Config] => Array[String] = toArgs)
                            (f: Config => Unit): Unit = {
    withProperties(toPropStr(props), toA(args))(f)
  }

  private def withProperties(propsStr: String,
                             argsArray: Array[String])
                            (f: Config => Unit): Unit = {
    val file = File.createTempFile(getClass.getName, "suffix")
    file.delete()
    file.mkdirs()
    file.deleteOnExit()
    System.setProperty("XB3_CONFIG_HOME", file.getAbsolutePath)
    val properties = new File(file, "ididthat.properties")
    properties.deleteOnExit()
    FileUtils.write(properties, propsStr)
    try {
      val loadedConfig = Config.load(argsArray)
      f(loadedConfig.configuration)
    } finally FileUtils.deleteDirectory(file)
  }

  private def toPropStr(cfg: Option[Config]): String = cfg match {
    case Some(config) => s"""
        |port=${config.port}
        |ldap-timeout=${config.ldapTimeout}
        |ldap-server-url=${config.ldapServerUrl}
        |ldap-admin-user-dn=${config.ldapAdminUserDn}
        |ldap-admin-user-password=${config.ldapAdminPass}
        |ldap-base=${config.ldapBase}
        |ldap-user-ou=${config.ldapUserOU}
        |mongo-host=${config.mongoHost}
        |mongo-port=${config.mongoPort}
        |mongo-user=${config.mongoUser}
        |mongo-pass=${config.mongoPass}
        |mongo-database=${config.mongoDatabase}
         """.stripMargin
    case None => ""
  }

  private def toArgs1(cfg: Option[Config]): Array[String] = cfg match {
    case Some(config) => Array(
      s"--port=${config.port}",
      s"--ldap-timeout=${config.ldapTimeout}",
      s"--ldap-server-url=${config.ldapServerUrl}",
      s"--ldap-admin-user-dn=${config.ldapAdminUserDn}",
      s"--ldap-admin-user-password=${config.ldapAdminPass}",
      s"--ldap-base=${config.ldapBase}",
      s"--ldap-user-ou=${config.ldapUserOU}",
      s"--mongo-host=${config.mongoHost}",
      s"--mongo-port=${config.mongoPort}",
      s"--mongo-user=${config.mongoUser}",
      s"--mongo-pass=${config.mongoPass}",
      s"--mongo-database=${config.mongoDatabase}"
    )
    case None => Array[String]()
  }

  private def toArgs(cfg: Option[Config]): Array[String] = cfg match {
    case Some(config) => Array(
      "--port", config.port + "",
      "--ldap-timeout", config.ldapTimeout + "",
      "--ldap-server-url", config.ldapServerUrl + "",
      "--ldap-admin-user-dn", config.ldapAdminUserDn + "",
      "--ldap-admin-user-password", config.ldapAdminPass + "",
      "--ldap-base", config.ldapBase +"",
      "--ldap-user-ou", config.ldapUserOU +"",
      "--mongo-host", config.mongoHost + "",
      "--mongo-port", config.mongoPort + "",
      "--mongo-user", config.mongoUser + "",
      "--mongo-pass", config.mongoPass +"",
      "--mongo-database", config.mongoDatabase +""
    )
    case None => Array[String]()
  }
}
