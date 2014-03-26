import sbt.Value

organization  := "com.nature"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "maven local"                 at Path.userHome.asFile.toURI.toURL + ".m2/repository",
  "Maven Central Server"        at "http://repo1.maven.org/maven2",
  "spray repo"                  at "http://repo.spray.io/",
  "mongodb async driver repo"   at "http://www.allanbank.com/repo/"
)

libraryDependencies ++= {
  val resourceLookupV = "2.6.0"
  val guiceV = "3.0"
  val akkaV = "2.2.3"
  val sprayV = "1.2.0"
  val sprayJsonV = "1.2.5"
  val springLdapV = "1.3.2.RELEASE"
  val mongoAsyncDriverV = "1.2.3"
  val embedMongoV = "0.6.0"
  val specs2V = "2.3.7"
  val scalatestV = "2.0"
  val mockitoV = "1.9.5"
  val festAssertV = "1.4"
  val guavaV = "r09"
  val commonsCliV = "1.2"
  Seq(
    "npg-commons"               %   "nc-resourcelookup"    % resourceLookupV exclude("com.google.code.guice", "guice"),
    "com.google.inject"         %   "guice"                % guiceV,
    "io.spray"                  %   "spray-can"            % sprayV,
    "io.spray"                  %   "spray-routing"        % sprayV,
    "io.spray"                  %   "spray-testkit"        % sprayV,
    "io.spray"                  %%  "spray-json"           % sprayJsonV,
    "org.springframework.ldap"  %   "spring-ldap-core"     % springLdapV,
    "com.allanbank"             %   "mongodb-async-driver" % mongoAsyncDriverV,
    "commons-cli"               %   "commons-cli"          % commonsCliV,
    "com.typesafe.akka"         %%  "akka-actor"           % akkaV,
    "com.typesafe.akka"         %%  "akka-testkit"         % akkaV,
    "org.scalatest"             %%   "scalatest"       % scalatestV  % "test",
    "org.mockito"               %   "mockito-all"          % mockitoV    % "test",
    "org.easytesting"           %   "fest-assert"          % festAssertV % "test",
    "com.google.guava"          %   "guava"                % guavaV      % "test",
    "com.github.simplyscala"    %% "scalatest-embedmongo"  % "0.2.1" % "test"
//    "de.flapdoodle.embed"       %   "de.flapdoodle.embed.mongo" % "1.42",
//    "org.mongodb"               %   "mongo-java-driver"         % "2.11.4"
  )
}

seq(Revolver.settings: _*)
