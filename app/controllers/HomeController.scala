package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.HomeController.{SwapEnd, SwapStart}
import play.api.http.ContentTypes
import play.api.mvc._
import scrapers.{SwapScraper, usps}

import java.time.ZoneId
import javax.inject._
import scala.xml.NodeSeq

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents, swapScraper: SwapScraper, mailpieceScraper: usps.MailpieceScraper)
    extends BaseController {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index(): Action[AnyContent] =
    Action {
      NotFound
    }

  def feed(): Action[AnyContent] =
    Action {
      val start = Source.single(SwapStart)

      val items = swapScraper.getListings.map { listing =>
        ByteString(
          <item>
              <title>{listing.title}</title>
              <guid isPermalink="false">{listing.id}</guid>
              <link>{listing.link}</link>
              <description><![CDATA[<p>{listing.subtitle}</p><p><img src="{listing.thumbnail}" alt="thumbnail" /></p>]]></description>
              {listing.endTime.fold(NodeSeq.Empty)(endTime => <pubDate>{endTime.atZone(ZoneId.of("US/Central")).toOffsetDateTime}</pubDate>)}
          </item>.toString()
        )
      }

      val end = Source.single(SwapEnd)

      Ok.chunked(start ++ items ++ end, Some(ContentTypes.XML))
    }

  def uspsMailpieces(): Action[AnyContent] =
    Action {
      Ok(views.xml.usps.mailpieces(mailpieceScraper.getMailpieces))
    }
}

object HomeController {
  private val SwapStart =
    ByteString(
      """<?xml version="1.0" encoding="UTF-8" ?>
        |<rss version="2.0">
        |
        |<channel>
        |    <title>UW SWAP Online Auction</title>
        |    <link>https://swapauction.wisc.edu/</link>
        |    <description>UW SWAP Online Auction</description>
        |""".stripMargin
    )

  private val SwapEnd =
    ByteString(
      """
        |</channel>
        |
        |</rss>
        |""".stripMargin
    )
}
