package controllers

import play.api.mvc._
import scrapers.{SwapScraper, usps}

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               swapScraper: SwapScraper,
                               mailpieceScraper: usps.MailpieceScraper
                              ) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action {
    NotFound
  }

  def feed(): Action[AnyContent] = Action {
    Ok(views.xml.feed(swapScraper.getListings))
  }

  def uspsMailpieces(): Action[AnyContent] = Action {
    Ok(views.xml.usps.mailpieces(mailpieceScraper.getMailpieces))
  }
}
