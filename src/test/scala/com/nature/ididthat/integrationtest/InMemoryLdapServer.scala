package com.nature.ididthat.integrationtest

import com.unboundid.ldap.listener.{InMemoryDirectoryServer, InMemoryDirectoryServerConfig, InMemoryListenerConfig}
import com.unboundid.ldap.sdk.{Attribute, Entry, DN}
import java.io.{InputStream, File}
import com.unboundid.ldif.LDIFReader

class InMemoryLdapServer(port: Int,
                         baseDN: String,
                         authDN: String,
                         authPassword: String,
                         ldifFile: String) {
  private var started = false
  private val listenerConfig = InMemoryListenerConfig.createLDAPConfig("default", port)
  private val config = new InMemoryDirectoryServerConfig(new DN(baseDN))
  config.setSchema(null)
  config.setListenerConfigs(listenerConfig)

  config.addAdditionalBindCredentials(authDN, authPassword)
  private val server: InMemoryDirectoryServer = new InMemoryDirectoryServer(config)
  server.add(new Entry(baseDN, new Attribute("objectclass", "domain", "top")))

  def start(): Unit = {
    this.synchronized {
      if (!started) {
        loadData(server, ldifFile)
        server.startListening()
        started = true
      }
    }
  }

  def stop(): Unit = {
    this.synchronized {
      if (started) {
        server.shutDown(true)
        started = false
      }
    }
  }

  /**
   * Load LDIF records from a file to seed the LDAP directory.
   *
   * @param server   The embedded LDAP directory server.
   * @param ldifFile The LDIF resource or file from which LDIF records will be loaded.
   * @throws com.unboundid.ldap.sdk.LDAPException
     * If there was a problem loading the LDIF records into the LDAP directory.
   * @throws java.io.IOException If there was a problem reading the LDIF records from the file.
   */
  private def loadData(server: InMemoryDirectoryServer, ldifFile: String) {
    if (ldifFile != null && !ldifFile.isEmpty) {
      if (new File(ldifFile).exists) {
        server.importFromLDIF(false, ldifFile)
      } else {
        val classLoader = Thread.currentThread.getContextClassLoader
        val inputStream = classLoader.getResourceAsStream(ldifFile)
        try {
          val reader = new LDIFReader(inputStream)
          server.importFromLDIF(true, reader)
        } finally {
          inputStream.close()
        }
      }
    }
  }
}
