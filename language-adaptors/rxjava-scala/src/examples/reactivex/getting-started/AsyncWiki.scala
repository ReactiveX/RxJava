import java.net.URL
import java.util.Scanner

import rx.lang.scala.Observable

object AsyncWiki extends App {
  /*
   * Fetch a list of Wikipedia articles asynchronously.
   */
  def fetchWikipediaArticleAsynchronously(wikipediaArticleNames: String*): Observable[String] = {
    Observable(subscriber => {
      new Thread(new Runnable() {
        def run() {
          for (articleName <- wikipediaArticleNames) {
            if (subscriber.isUnsubscribed) {
              return
            }
            val url = "http://en.wikipedia.org/wiki/" + articleName
            val art = new Scanner(new URL(url).openStream()).useDelimiter("\\A").next()
            subscriber.onNext(art)
          }
          if (!subscriber.isUnsubscribed) {
            subscriber.onCompleted()
          }
        }
      }).start()
    })
  }

  fetchWikipediaArticleAsynchronously("Tiger", "Elephant")
    .subscribe(art => println("--- Article ---\n" + art.substring(0, 125)))
}
