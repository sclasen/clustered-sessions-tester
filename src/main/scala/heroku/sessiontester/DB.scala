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

  def addTestRun(appUrl: String, appStack: String, dynos: Int, requests: Int, counted: Int, concurrency: Int, scale: Int, think: Int, weight: Int) {
    pool.foreach {
      p =>
        managed(p.getConnection).acquireAndGet {
          c => managed(c.prepareStatement(""" INSERT INTO RESULTS VALUES(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP) """)).acquireAndGet {
            s =>
              import s._
              setString(1, appUrl)
              setString(2, appStack)
              setInt(3, dynos)
              setInt(4, requests)
              setInt(5, counted)
              setInt(6, concurrency)
              setInt(7, scale)
              setInt(8, think)
              setInt(9, weight)
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
                    app varchar(1024),
                    stack varchar(1024),
                    dynos bigint,
                    requests bigint,
                    counted bigint,
                    concurrency bigint,
                    scale bigint,
                    think bigint,
                    weight bigint,
                    added timestamp
                    )
                    """)
      println(" CREATE TABLE RESULTS")
      addTestRun("test", "test", 1, 1, 1, 1, 1, 1, 1)
      println("test insert passed")
      stmt.executeUpdate("delete from RESULTS")
      println("cleared table")
    }
  }

}