package util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import java.net.{URL, URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._
import scala.util.Try

object SwapScraper {
  private val BaseUrl: URL = new URL("https", "swapauction.wisc.edu", "/Browse")
  private val BaseParameters: Map[String, String] = Map(
    "ViewStyle" -> "list",
    "ListingType" -> "Auction,FixedPrice",
    "StatusFilter" -> "active_only",
    "SortFilterOptions" -> "1"
  )
  private val ListingIdAttribute = "data-listingid"
  private val EndTimeAttribute = "data-action-time"
  private val EndTimeFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")

  case class Listing(id: String, title: String, subtitle: String, link: URL, thumbnail: String, endTime: Option[LocalDateTime])

  object Listing {
    def apply(element: Element): Listing = {
      val id = element.attr(ListingIdAttribute)
      val title = element.select(".title").text()
      val subtitle = element.select(".subtitle").text()
      val link = new URL(element.select(".bids > a").last().absUrl("href"))
      val thumbnail = element.select("img").first().absUrl("src")
      val endTimeStr = element.getElementsByAttribute(EndTimeAttribute).attr(EndTimeAttribute)
      val endTime = Try(LocalDateTime.parse(endTimeStr, EndTimeFormat)).toOption
      Listing(id, title, subtitle, link, thumbnail, endTime)
    }
  }

  def getListings: Seq[Listing] = getListings(None)

  private def getListings(page: Option[Int]): Seq[Listing] = {
    val queryParameters = buildQueryParams(BaseParameters ++ page.map("page" -> _.toString))
    val doc = Jsoup.connect(s"$BaseUrl?$queryParameters").get()
    val listings = doc.getElementsByAttribute(ListingIdAttribute)
    val nextPage = doc.select("ul.pagination > li").last()
    val nextPageHref = Option.when(!nextPage.hasClass("disabled"))(nextPage.firstElementChild().attr("href"))
    val nextPageNumber = nextPageHref.map(extractQueryParams).flatMap(_.get("page")).flatMap(_.toIntOption)
    listings.asScala.toSeq.map(Listing(_)) ++ (if (nextPageNumber.isDefined) getListings(nextPageNumber) else Nil)
  }

  private def urlEncode(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)

  private def urlDecode(s: String): String = URLDecoder.decode(s, StandardCharsets.UTF_8)

  private def extractQueryParams(url: String): Map[String, String] =
    url.split('?').last.split('&').map(_.split("=", 2)).collect {
      case Array(k, v) => urlDecode(k) -> urlDecode(v)
    }.toMap

  private def buildQueryParams(params: Map[String, String]): String =
    params.map { case (k, v) => s"${urlEncode(k)}=${urlEncode(v)}" }.mkString("&")
}
