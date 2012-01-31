package heroku.sessiontester

import com.twitter.util.Future
import com.twitter.finagle.builder.ClientBuilder
import java.net.{InetSocketAddress, URL}
import com.twitter.finagle.http.Http
import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import com.codahale.jerkson.Json._
import java.lang.String
import org.jboss.netty.util.CharsetUtil
import util.{Random, Properties}


case class Session(count: Int)

object Run {

  val debug = Properties.envOrElse("DEBUG", "false").toBoolean
  val think = Properties.envOrNone("THINK").map(_.toLong)
  val appStack = Properties.envOrElse("APP_STACK", "UNKNOWN")
  val dynos = Properties.envOrElse("DYNOS", "0").toInt
  val scale = Properties.envOrElse("SCALE", "0").toInt


  def main(args: Array[String]) {
    val concurrency = Properties.envOrElse("CONCURRENCY", "1").toInt
    val numRequests = Properties.envOrElse("REQUESTS", "1").toInt
    val url = Properties.envOrNone("APP_URL").get
    val start = System.currentTimeMillis()
    val futures = (1 to concurrency) map (x => go(x, numRequests, url))
    println("Started %d pipelines of %d requests".format(concurrency, numRequests))
    val overall = concurrency * numRequests
    Future.collect(futures).get().foreach {
      lastResp => {
        val session: Session = parse[Session](lastResp._1.getContent.array())
        DB.addTestRun(url, appStack, dynos, numRequests, session.count, concurrency, scale, think)
        if (session.count == numRequests) {
          if (debug)
            println("Client #%d Session was consistent".format(lastResp._2))
        } else {
          println("Client #%d Session was inconsistent made %d requests but only %d count".format(lastResp._2, numRequests, session.count))
        }
      }
    }
    val time = System.currentTimeMillis() - start
    println("Executed %d total requests".format(overall))
    println("Total time: %d ms".format(time))
    println("RPS: %d ".format((overall * 1000) / time))
    think.foreach(t => println("Think Time at least:%d ms".format(t)))
  }


  def go(id: Int, numRequests: Int, app: String): Future[(HttpResponse, Int)] = {
    val url = new URL(app)
    val client: Service[HttpRequest, HttpResponse] = ClientBuilder().
      hosts(new InetSocketAddress(url.getHost, 80)).codec(Http.get()).name(id.toString).hostConnectionLimit(1).build()
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, url.getPath);
    req.setHeader(HttpHeaders.Names.HOST, url.getHost)
    client(req).flatMap {
      resp =>
        val cookieHeader = resp.getHeader(HttpHeaders.Names.SET_COOKIE)
        val cookie = cookieHeader.substring(0, cookieHeader.indexOf(";"))
        req.setHeader(HttpHeaders.Names.COOKIE, cookie)
        requests(req)(client, numRequests - 1).map((_, id))
    }
  }

  def requests(req: HttpRequest, count: Int = 0)(implicit client: Service[HttpRequest, HttpResponse], total: Int): Future[HttpResponse] = {
    if (total - count > 0) {
      client(req).flatMap {
        resp =>
          printResp(resp)
          think.foreach {
            t =>
              val sleep = t.toInt + Random.nextInt(t.toInt)
              if (debug) println("Sleeping:" + sleep)
              Thread.sleep(sleep.toLong)
              if (debug) println("done")

          }
          requests(req, count + 1)
      }
    } else {
      client(req).onSuccess(resp => client.release())
    }
  }

  def printResp(resp: HttpResponse) {
    if (debug) {
      println("==>" + resp.getContent.toString(CharsetUtil.UTF_8))
    }
  }
}

