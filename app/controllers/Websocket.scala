package controllers

import scala.concurrent.duration.DurationInt
import scala.concurrent.future

import actors.GithubFriends
import actors.StringMsg
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Iteratee
import play.api.mvc.Controller
import play.api.mvc.WebSocket

object Websocket extends Controller {

  val worker = Akka.system.actorOf(Props[GithubFriends])
  
  implicit val timeout = Timeout(5 seconds)
  
  def index = WebSocket.async[String] { request =>
    future {
    	val (outenum, outchannel) = Concurrent.broadcast[String]

	    var in = Iteratee.foreach[String]{
	      s => (worker ? StringMsg(s)).map {
	        	case StringMsg(stringMsg) => outchannel.push(stringMsg)
	      }  			
	      println(s)
	    }.mapDone { _ =>
	      println("Disconnected")
	    }
	    (in, outenum)
    }
  }

  class Reverser extends Actor {
    
    import context._
    
    override def receive = receiveAndReverse
    
    def receiveAndReverse : Receive = {
      case StringMsg(s) => {
        println(s"Got msg ${s}")
        sender ! StringMsg(s.reverse)
        become(beAwkward)
      }
    }
    
    def beAwkward : Receive = {
      case StringMsg(s) => {
        sender ! StringMsg("badger")
        become(receiveAndReverse)
      }
    }
  }
  
  

  

}