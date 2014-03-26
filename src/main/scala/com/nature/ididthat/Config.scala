package com.nature.ididthat

import com.nature.components.service.resources.ResourceLookUp
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.ldap.core.LdapTemplate
import akka.actor._
import java.util.Properties
import java.util
import org.apache.commons.cli._
import com.allanbank.mongodb.{Credential, MongoClientConfiguration}

case class Config(port: Int, //TODO: perhaps multiple constructors, with default mongo
                  ldapTimeout: Int,
                  ldapServerUrl: String,
                  ldapAdminUserDn: String,
                  ldapAdminPass: String,
                  ldapBase: String,
                  ldapUserOU: String,

                  mongoHost: String,
                  mongoPort: Int,
                  mongoUser: String,
                  mongoPass: String,
                  mongoDatabase: String)

class ConfigModule(config: Config) {
  private val contextSource = new LdapContextSource()
  contextSource.setUrl(config.ldapServerUrl)
  contextSource.setUserDn(config.ldapAdminUserDn)
  contextSource.setPassword(config.ldapAdminPass)
  contextSource.setPooled(false)
  contextSource.afterPropertiesSet()
  private val template = new LdapTemplate(contextSource)
  template.setIgnorePartialResultException(false)
  private val ldapAuthenticator = new LdapAuthenticator(template, config)

  def configuration = config

  def ldapContextSource = contextSource

  def ldapTemplate = template

  def authenticator = ldapAuthenticator

  private val mongoConfig = new MongoClientConfiguration()
  mongoConfig.addServer(s"${config.mongoHost}:${config.mongoPort}")
  mongoConfig.addCredential(Credential.builder()
    .userName(config.mongoUser)
    .password(config.mongoPass.toCharArray)
    .database(config.mongoDatabase).build()
  )

  def mongoConfiguration = mongoConfig
}

trait ActorInjector {
  def configModule: ConfigModule

  def logInActor(context: ActorRefFactory): ActorRef =
    context.actorOf(Props(classOf[LogInActor], configModule))
}

object Config {
  final val Help = "help"
  final val Port = "port"
  final val LdapTimeout = "ldap-timeout"
  final val LdapServerUrl = "ldap-server-url"
  final val LdapAdminUserDn = "ldap-admin-user-dn"
  final val LdapAdminUserPassword = "ldap-admin-user-password"
  final val LdapBase = "ldap-base"
  final val LdapUserOu = "ldap-user-ou"

  final val HelpMessage = "Runs the 'Ididthat' web application"

  final val DefaultPort = 9427
  final val DefaultLdapTimeout = 2000

  final val MongoPort = "12345"
  final val DefaultMongoPort = 12345
  final val MongoHost = "mongo-host"
  final val MongoUser = "mongo-user"
  final val MongoPass = "mongo-pass"
  final val MongoDatabase = "mongo-database"

  def load(args: Array[String]): ConfigModule = {
    val resourceLookup = new ResourceLookUp("ididthat")
    val properties = resourceLookup.getProperties match {
      case p: Properties => p
      case _ => new Properties
    }
    load(properties, args)
  }

  def load(properties: Properties, args: Array[String]): ConfigModule = {
    val line = parseCmdLine(args)

    def getValue(prop: String): String = {
      line.getOptionValue(prop, properties.getProperty(prop))
    }

    val port = intDefault(getValue(Port), DefaultPort)
    val ldapTimeout = intDefault(getValue(LdapTimeout), DefaultLdapTimeout)
    val ldapServerUrl = checkNotNull(getValue(LdapServerUrl), LdapServerUrl)
    val ldapAdminUserDn = checkNotNull(getValue(LdapAdminUserDn), LdapAdminUserDn)
    val ldapAdminPass = checkNotNull(getValue(LdapAdminUserPassword), LdapAdminUserPassword)
    val ldapBase = checkNotNull(getValue(LdapBase), LdapBase)
    val ldapUserOU = checkNotNull(getValue(LdapUserOu), LdapUserOu)

    //TODO: do we fall back on defaults here? for now, I am providing in the config file
    val mongoHost = checkNotNull(getValue(MongoHost), MongoHost)
    val mongoPort = intDefault(getValue(MongoPort), DefaultMongoPort)
    val mongoUser = checkNotNull(getValue(MongoUser), MongoUser)
    val mongoPass = checkNotNull(getValue(MongoPass), MongoPass)
    val mongoDatabase = checkNotNull(getValue(MongoDatabase), MongoDatabase)

//    val mongoHost = properties.getProperty("mongo-host")
//    val mongoPort = properties.getProperty("mongo-port") match {
//      case p: String => try p.toInt catch {
//        case e: NumberFormatException => throw new IllegalArgumentException("The property mongo-port must be an integer")
//      }
//      case _ => throw new IllegalArgumentException("Please specify mandatory property mongo-port")
//    }
//    val mongoUser = properties.getProperty("mongo-user")
//    val mongoPass = properties.getProperty("mongo-pass")
//    val mongoDatabase = properties.getProperty("mongo-database")

    new ConfigModule(new Config(
      port,
      ldapTimeout,
      ldapServerUrl,
      ldapAdminUserDn,
      ldapAdminPass,
      ldapBase,
      ldapUserOU,
      mongoHost,
      mongoPort,
      mongoUser,
      mongoPass,
      mongoDatabase
    )
    )
  }

  private def checkNotNull(value: String, prop: String): String = {
    if (value == null)
      throw new IllegalArgumentException(s"Please provide mandatory property/cmdarg '$prop'")
    else value
  }

  def intDefault(value: String, default: Integer): Int = value match {
    case p: String => try p.toInt catch {
      case e: NumberFormatException => default
    }
    case _port => default
  }

  private def parseCmdLine(args: Array[String]): CommandLine = {
    val formatter = new HelpFormatter()
    formatter.setWidth(Integer.MAX_VALUE)
    val parser = new GnuParser() {
      override def processOption(option: String, iter: util.ListIterator[_]) {
        if (getOptions.hasOption(option)) super.processOption(option, iter)
      }
    }

    val options = new Options
    options.addOption("h", Help, false, "Prints a help message and explanation")
    options.addOption("p", Port, true, s"Prints the output in a browser readable format. Default value $DefaultPort.")
    options.addOption("lto", LdapTimeout, true, s"The time in milliseconds to wait for the Ldap Server response. Default value $DefaultLdapTimeout")
    options.addOption("lu", LdapServerUrl, true, "The url where the Ldap server is running. Mandatory argument.")
    options.addOption("ldn", LdapAdminUserDn, true, "The Ldap admin user DN. Mandatory argument.")
    options.addOption("lp", LdapAdminUserPassword, true, "The admin password. Mandatory argument.")
    options.addOption("lb", LdapBase, true, "Ldap Base DN. Mandatory argument.")
    options.addOption("lou", LdapUserOu, true, "Ldap User Ou. Mandatory argument.")
    options.addOption("mh", MongoHost, true, "MongoDB Host. Mandatory argument.")
    options.addOption("mp", MongoPort, true, "MongoDB Port. Mandatory argument.")
    options.addOption("muser", MongoUser, true, "MongoDB User. Mandatory argument.")
    options.addOption("mpass", MongoPass, true, "MongoDB Password. Mandatory argument.")
    options.addOption("mdb", MongoDatabase, true, "MongoDB Database. Mandatory argument.")


    try {
      val line = parser.parse(options, args, false)

      if (line.hasOption(Help)) {
        formatter.printHelp(HelpMessage, options)
        sys.exit(0)
      }

      line
    } catch {
      case e: ParseException =>
        formatter.printHelp(HelpMessage, options)
        throw e
    }
  }
}
