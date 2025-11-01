// package session

// import java.time.{Duration, Instant}
// import java.util.UUID
// import javax.servlet.http.Cookie

// import scala.util.{Try, Success, Failure}

// // Placeholder imports for JWT handling, adjust for your stack
// import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
// import play.api.libs.json.Json

// // Placeholder Config classes
// case class CookieConfig(
//   name: String,
//   domain: String,
//   httpOnly: Boolean,
//   sameSite: String,
//   secure: Boolean
// )

// case class SessionConfig(
//   lifespan: String,
//   cookie: CookieConfig,
//   issuer: String,
//   audience: Option[Seq[String]],
//   jwtTemplate: Option[JWTTemplate]
// )

// case class JWTTemplate(claims: Map[String, String])

// case class Config(
//   session: SessionConfig,
//   webauthn: WebauthnConfig
// )

// case class WebauthnConfig(relyingParty: RelyingPartyConfig)
// case class RelyingPartyConfig(id: String)

// // Placeholder UserJWT, adjust as needed
// case class UserJWT(userID: String, email: Option[String], username: String)

// // JWTOptions type alias
// type JWTOptions = JwtClaim => JwtClaim

// trait Manager {
//   def generateJWT(user: UserJWT, opts: JWTOptions*): Try[(String, JwtClaim)]
//   def verify(token: String): Try[JwtClaim]
//   def generateCookie(token: String): Cookie
//   def deleteCookie(): Cookie
// }

// class SessionManager(
//   signatureKey: String,
//   verificationKeys: Seq[String],
//   sessionLength: Duration,
//   cookieConfig: CookieConfig,
//   issuer: String,
//   audience: Seq[String],
//   jwtTemplate: Option[JWTTemplate]
// ) extends Manager {

//   private val algo = JwtAlgorithm.HS256 // Adjust for your use case

//   override def generateJWT(user: UserJWT, opts: JWTOptions*): Try[(String, JwtClaim)] = {
//     val now = Instant.now()
//     val exp = now.plus(sessionLength)
//     val sessionId = UUID.randomUUID().toString

//     // Start with template claims if present
//     val baseClaims = jwtTemplate
//       .map { tmpl =>
//         tmpl.claims ++ Map(
//           "sub" -> user.userID,
//           "iat" -> now.getEpochSecond.toString,
//           "exp" -> exp.getEpochSecond.toString,
//           "aud" -> Json.toJson(audience).toString,
//           "session_id" -> sessionId
//         )
//       }
//       .getOrElse(
//         Map(
//           "sub" -> user.userID,
//           "iat" -> now.getEpochSecond.toString,
//           "exp" -> exp.getEpochSecond.toString,
//           "aud" -> Json.toJson(audience).toString,
//           "session_id" -> sessionId
//         )
//       )

//     val withEmail =
//       user.email.map(email => baseClaims + ("email" -> email)).getOrElse(baseClaims)
//     val withUsername =
//       if (user.username.nonEmpty) withEmail + ("username" -> user.username) else withEmail
//     val withIssuer =
//       if (issuer.nonEmpty) withUsername + ("iss" -> issuer) else withUsername

//     val initialClaim = JwtClaim(Json.stringify(Json.toJson(withIssuer)))

//     // Apply additional options
//     val finalClaim = opts.foldLeft(initialClaim)((claim, opt) => opt(claim))

//     Jwt.encode(finalClaim, signatureKey, algo) match {
//       case token: String => Success((token, finalClaim))
//       case _ => Failure(new Exception("Failed to sign JWT"))
//     }
//   }

//   override def verify(token: String): Try[JwtClaim] = {
//     Jwt.decode(token, signatureKey, Seq(algo)).toTry
//   }

//   override def generateCookie(token: String): Cookie = {
//     val cookie = new Cookie(cookieConfig.name, token)
//     cookie.setDomain(cookieConfig.domain)
//     cookie.setPath("/")
//     cookie.setHttpOnly(cookieConfig.httpOnly)
//     cookie.setSecure(cookieConfig.secure)
//     cookie.setMaxAge(sessionLength.getSeconds.toInt)
//     // Handle SameSite if your framework supports it
//     cookie
//   }

//   override def deleteCookie(): Cookie = {
//     val cookie = new Cookie(cookieConfig.name, "")
//     cookie.setDomain(cookieConfig.domain)
//     cookie.setPath("/")
//     cookie.setHttpOnly(cookieConfig.httpOnly)
//     cookie.setSecure(cookieConfig.secure)
//     cookie.setMaxAge(-1)
//     // Handle SameSite if your framework supports it
//     cookie
//   }
// }

// object SessionManager {
//   def apply(jwkManager: JwkManager, config: Config): Try[Manager] = {
//     for {
//       signatureKey <- jwkManager.getSigningKey
//       verificationKeys <- jwkManager.getPublicKeys
//       sessionLength <- Try(Duration.parse(s"PT${config.session.lifespan.toUpperCase}"))
//       audience = config.session.audience.getOrElse(Seq(config.webauthn.relyingParty.id))
//       manager = new SessionManager(
//         signatureKey,
//         verificationKeys,
//         sessionLength,
//         config.session.cookie,
//         config.session.issuer,
//         audience,
//         config.session.jwtTemplate
//       )
//     } yield manager
//   }
// }

// // Placeholder JWK manager
// trait JwkManager {
//   def getSigningKey: Try[String]
//   def getPublicKeys: Try[Seq[String]]
// }

// // Example JWTOptions helper
// object JWTOptions {
//   def withValue(key: String, value: String): JWTOptions = { claim =>
//     val json = Json.parse(claim.content)
//     val updated = json.as[Map[String, String]] + (key -> value)
//     claim.copy(content = Json.stringify(Json.toJson(updated)))
//   }
// }