package util

import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.{By, WebElement}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._
import scala.util.Try

object SwapScraper {
  private val StartingUrl = "https://swapauction.wisc.edu/Browse?ViewStyle=list&ListingType=Auction,FixedPrice&StatusFilter=active_only&SortFilterOptions=1"
  private val ListingIdAttribute = "data-listingid"
  private val EndTimeAttribute = "data-action-time"
  private val EndTimeFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")

  private val ExecuteHeadless = true

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

  def getListings: Seq[Listing] = {
    val options = new ChromeOptions
    if (ExecuteHeadless) options.addArguments("--headless=new")
    val driver = new ChromeDriver(options)
    try {
      driver.get(StartingUrl)
      getListingElements(driver)
    } finally {
      driver.quit()
    }
  }

  private def getListingElements(driver: ChromeDriver): Seq[Listing] = {
    val listingElements = driver.findElements(By.cssSelector(s"[$ListingIdAttribute]"))
    val nextPage = driver.findElement(By.cssSelector("ul.pagination > li:last-child"))
    val results = listingElements.asScala.toSeq.map { element =>
      if (!ExecuteHeadless) driver.executeScript("arguments[0].scrollIntoView(true);", element)
      Listing(element)
    }
    if (nextPage.getAttribute("class").split(" ").contains("disabled")) {
      results
    } else {
      nextPage.findElement(By.tagName("a")).click()
      results ++ getListingElements(driver)
    }
  }
}
