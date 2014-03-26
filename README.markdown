## I Did This
This is a simple application having similar functionality as https://idonethis.com/ but with the flavour of Nature and the Nature's internal authentication mechanism.

The initial plan of the application is outlined bellow:
The application has a service layer exposed as JSON Rest services. The UI technology of the application is yet to be defined.

### Technology stack
* Scala
* Spray
* AKKA
* MongoDb
 
The whole of the application is designed on asynchronous services using the AKKA and MongoDB asynchronos driver. This serves two purpouses
* Being able to serve very high number of requests
* Research new technologies

#### Authentiction
* HTTPS for the login page
* We need a service that is being able to authenticate the user based on the existing Nature Ldap accounts.
* After authentication a cookie with the outhentication period and the user name is set on the user machine.
  * The coockie is encripted
  * The coockie is padded along with each reques so the server can authenticate the user. The coocky is set after sucessful authentication.
  * On the server there is a filter that checks that coockie and redirects either to the requested services or to the authentication service

#### Deployment
* RPM based
* Service script

#### Persistence
TBD
