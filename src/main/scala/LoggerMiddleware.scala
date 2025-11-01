// import cats.data.Kleisli
// import cats.effect.{Clock, Sync}
// import cats.implicits._
// import org.http4s.{HttpApp, Request, Response}
// import org.typelevel.log4cats.Logger

// import java.time.Instant

// object LoggerMiddleware {

//   def apply[F[_]: Sync: Clock](logger: Logger[F]): HttpApp[F] => HttpApp[F] =
//     httpApp =>
//       Kleisli { req: Request[F] =>
//         for {
//           start <- Clock[F].realTime
//           resp  <- httpApp(req).attempt
//           end   <- Clock[F].realTime
//           latency = end - start
//           now <- Sync[F].delay(Instant.now())
//           // Gather request and response data
//           method = req.method.name
//           uri = req.uri.renderString
//           remoteIp = req.remoteAddr.getOrElse("-")
//           userAgent = req.headers.get(org.http4s.headers.`User-Agent`).fold("-")(_.value)
//           referer = req.headers.get(org.http4s.headers.Referer).fold("-")(_.value)
//           status = resp.fold(_ => 500, _.status.code)
//           error = resp.swap.toOption.map(_.getMessage).getOrElse("")
//           // Optionally, gather bytes in/out (not available out-of-the-box)
//           _ <- logger.info(
//             s"""{"time":"$now","time_unix":"${now.getEpochSecond}","remote_ip":"$remoteIp","method":"$method","uri":"$uri","user_agent":"$userAgent","status":$status,"error":"$error","latency":$latency,"referer":"$referer"}"""
//           )
//           out <- resp.liftTo[F]
//         } yield out
//       }
// }