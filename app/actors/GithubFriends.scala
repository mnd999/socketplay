package actors
import concurrent.duration._
import concurrent._
import spray.can.Http
import spray.http._
import spray.httpx.unmarshalling._
import spray.util._
import HttpMethods._
import play.api.libs.json.Json
import spray.httpx.PlayJsonSupport
import play.api.libs.json.JsValue
import play.api.libs.json.JsArray
import akka.actor.Actor
import akka.agent.Agent
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

class GithubFriends extends Actor with PlayJsonSupport {
  
    import context._
    
    val gitHubUrl = "https://api.github.com/users/mnd999/following"
    val agent = Agent(GitHubFriendList(List()))(Akka.system)
    
    implicit val timeout = Timeout(5 seconds)
    
    override def receive = receiveFirst
    
    def receiveFirst : Receive = {
      case StringMsg(s) => {
    	agent.sendOff(updateAgent)
        sender ! StringMsg(agent.await.friends.mkString(","))
        system.scheduler.schedule(1 minute, 1 minute)( agent.sendOff(updateAgent))
        become(receiveLater)
      }
    }
      
    def receiveLater : Receive = {
      case StringMsg(s) => {
        sender ! StringMsg(agent.get.friends.mkString(", "))
      } 
    }
    
    private def updateAgent(list : GitHubFriendList) : GitHubFriendList  = {
      println("Updating friend list")
      
      val response: Future[HttpResponse] =
    		  (IO(Http) ? HttpRequest(GET, Uri(gitHubUrl))).mapTo[HttpResponse]
      
      val json : Future[Seq[String]] = response.map(response => response.entity.as[JsArray] match {
        case Right(json) => extractUsernameFromJson(json)
        case Left(error) => List()
      })
      
      GitHubFriendList(Await.result(json, 5 seconds))
    }
    
    private def extractUsernameFromJson(json : JsArray) : Seq[String] = {
      json.value.map(js => (js \ "login").as[String])
    }
     
  	case class GitHubFriendList(friends : Seq[String])
  }