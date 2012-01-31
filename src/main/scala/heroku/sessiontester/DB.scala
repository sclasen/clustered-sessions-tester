package heroku.sessiontester

import util.Properties
import java.net.URI
import org.postgresql.ds.PGPoolingDataSource
import resource._

object DB {

  val pool = Properties.envOrNone("DATABASE_URL").map {
    db =>
      val url = new URI(db)
      val auth = url.getUserInfo.split(":")
      val user = auth(0)
      val pass = auth(1)
      val pool = new PGPoolingDataSource()
      pool.setServerName(url.getHost)
      pool.setDatabaseName(url.getPath.substring(1))
      pool.setUser(user)
      pool.setPassword(pass)
      pool.setDataSourceName("pg")
      pool
  }

  def addTestRun(appStack: String, dynos: Int, requests: Int, counted: Int, concurrency: Int, scale: Int) {
    pool.foreach {
      p =>
        managed(p.getConnection).acquireAndGet {
          c => managed(c.prepareStatement(""" INSERT INTO RESULTS VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP) """)).acquireAndGet {
            s =>
              import s._
              setString(1, appStack)
              setInt(2, dynos)
              setInt(3, requests)
              setInt(4, counted)
              setInt(5, concurrency)
              setInt(6, scale)
              executeUpdate()
          }
        }
    }
  }

  def main(args: Array[String]) {
    val p = pool.get
    for (conn <- managed(p.getConnection);
         stmt <- managed(conn.createStatement())
    ) {
      stmt.execute("DROP TABLE RESULTS")
      println("DROP TABLE RESULTS")
      stmt.execute("""
                    CREATE TABLE RESULTS
                    (
                    stack varchar(1024),
                    dynos bigint,
                    requests bigint,
                    counted bigint,
                    concurrency bigint,
                    scale bigint,
                    added timestamp
                    )
                    """)
      println(" CREATE TABLE RESULTS")
    }
  }

}