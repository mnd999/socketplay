package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Concurrent
import akka.actor.Actor
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._
import concurrent._

object Websocket extends Controller {

  val reverser = Akka.system.actorOf(Props[Reverser])
  
  def index = WebSocket.async[String] { request =>
    future {
    	val (outenum, outchannel) = Concurrent.broadcast[String]
    	implicit val timeout = Timeout(5 seconds)

	    var in = Iteratee.foreach[String]{
	      s => (reverser ? StringMsg(s)).map {
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
 
  case class StringMsg(str : String)
}