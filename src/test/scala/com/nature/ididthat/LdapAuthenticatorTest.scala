package com.nature.ididthat

import org.scalatest.FunSuite
import scala.util.{Failure, Success}
import org.springframework.ldap.core.{AttributesMapper, LdapTemplate}
import org.mockito.Mockito._
import org.mockito.Matchers._
import javax.naming.Name
import com.google.common.collect.Lists
import org.fest.assertions.Assertions._
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

class LdapAuthenticatorTest extends FunSuite {
  val ldapTempl = mock(classOf[LdapTemplate])

  val auth = new LdapAuthenticator(ldapTempl, ConfigTest.mockConfig)
  val user = mock(classOf[User])

  ignore("Real successful authentication") {
    val module = new ConfigModule(new Config(
      Config.DefaultPort,
      Config.DefaultLdapTimeout,
      "ldap://192.168.88.86:389",
      "uid=pcadmin-admin,ou=administrators,ou=topologymanagement,o=netscaperoot",
      "pcadmin321",
      "dc=nature,dc=com",
      "pcadmin",
      "localhost",
      12345,
      "test",
      "pass",
      "testDB")
    )
    val authenticator = new LdapAuthenticator(module.ldapTemplate, module.configuration)

    authenticator.authenticate("lcekov", "") match {
      case Success(lcekov) => println("success")
      case Failure(e) => throw e
    }
  }

  test("Authenticated") {
    when(ldapTempl.authenticate(any(classOf[Name]), anyString(), anyString()))
      .thenReturn(true)
    when(ldapTempl.search(any(classOf[Name]), anyString, any(classOf[AttributesMapper])))
      .thenAnswer(new Answer[java.util.List[_]]() {
      override def answer(inv: InvocationOnMock): java.util.List[_] = Lists.newArrayList(user)
    })

    val Success(u) = auth.authenticate("test", "pass")
    assertThat(u).isSameAs(user)
  }

  test("Not authenticated") {
    when(ldapTempl.authenticate(any(classOf[Name]), anyString(), anyString()))
      .thenReturn(false)
    val Failure(e) = auth.authenticate("test", "pass")
    assertThat(e).isInstanceOf(classOf[Exception])
  }

  test("Missing parameter") {
    when(ldapTempl.authenticate(any(classOf[Name]), anyString(), anyString()))
      .thenReturn(false)
    val Failure(e) = auth.authenticate("test", null)
    assertThat(e).isInstanceOf(classOf[Exception])

    val Failure(e1) = auth.authenticate(null, null)
    assertThat(e1).isInstanceOf(classOf[Exception])

    val Failure(e2) = auth.authenticate("", "")
    assertThat(e2).isInstanceOf(classOf[Exception])

  }

  test("Exception during authentication") {
    val e = new RuntimeException()
    when(ldapTempl.authenticate(any(classOf[Name]), anyString(), anyString()))
      .thenThrow(e)
    val Failure(ex) = auth.authenticate("test", "pass")
    assertThat(ex).isSameAs(e)
  }
}
