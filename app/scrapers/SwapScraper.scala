package scrapers

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.{By, WebElement}
import play.api.inject.Injector

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SwapScraper @Inject() (injector: Injector)(implicit ec: ExecutionContext) {
  def getListings: Source[SwapScraper.Listing, _] =
    Source.queue(1000, OverflowStrategy.fail).mapMaterializedValue { queue: SourceQueueWithComplete[SwapScraper.Listing] =>
      val driver = injector.instanceOf[RemoteWebDriver]
      driver.get(SwapScraper.StartingUrl)
      SwapScraper.scrapeListingElements(driver, queue)
      NotUsed
    }
}

object SwapScraper {
  private val StartingUrl = "https://swapauction.wisc.edu/Browse?ViewStyle=list&ListingType=Auction,FixedPrice&StatusFilter=active_only&SortFilterOptions=1"
  private val ListingIdAttribute = "data-listingid"
  private val EndTimeAttribute = "data-action-time"
  private val EndTimeFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")

  case class Listing(id: String, title: String, subtitle: String, link: String, thumbnail: String, endTime: Option[LocalDateTime])

  object Listing {
    def apply(element: WebElement): Listing = {
      val id = element.getAttribute(ListingIdAttribute)
      val title = element.findElement(By.className("title")).getText
      val subtitle = element.findElement(By.className("subtitle")).getText
      val link = element.findElement(By.cssSelector(".bids > a:last-child")).getAttribute("href")
      val thumbnail = element.findElement(By.tagName("img")).getAttribute("src")
      val endTime = for {
        endTimeElement <- Try(element.findElement(By.cssSelector(s"[$EndTimeAttribute]")))
        endTimeStr <- Try(endTimeElement.getAttribute(EndTimeAttribute))
        time <- Try(LocalDateTime.parse(endTimeStr, EndTimeFormat))
      } yield time
      Listing(id, title, subtitle, link, thumbnail, endTime.toOption)
    }
  }

  private def scrapeListingElements(driver: RemoteWebDriver, queue: SourceQueueWithComplete[Listing])(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      val listingElements = driver.findElements(By.cssSelector(s"[$ListingIdAttribute]"))
      listingElements.forEach { element =>
        driver.executeScript("arguments[0].scrollIntoView(true)", element)
        queue.offer(Listing(element))
      }
    }.transformWith {
      case Success(_) =>
        val nextPage = driver.findElement(By.cssSelector("ul.pagination > li:last-child"))
        if (nextPage.getAttribute("class").split(" ").contains("disabled")) {
          Future.successful {
            queue.complete()
            driver.close()
          }
        } else {
          nextPage.findElement(By.tagName("a")).click()
          scrapeListingElements(driver, queue)
        }
      case Failure(exception) =>
        queue.fail(exception)
        Future.failed(exception)
    }
  }
}
