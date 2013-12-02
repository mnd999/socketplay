package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import scala.collection.mutable.Stack
import play.api.libs.iteratee.Concurrent
import akka.actor.Actor
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._

object Websocket extends Controller {

  val reverser = Akka.system.actorOf(Props[Reverser])
  
  def index = WebSocket.using[String] { request =>

    val (outenum, outchannel) = Concurrent.broadcast[String]
    implicit val timeout = Timeout(5 seconds)
    
    val in = Iteratee.foreach[String]{
      s => (reverser ? StringMsg(s)).map {
        	case StringMsg(stringMsg) => outchannel.push(stringMsg)
      }  			
      println(s)
    }.mapDone { _ =>
      println("Disconnected")
    }

    (in, outenum)
  }

  class Reverser extends Actor {
    
    def receive = {
      case StringMsg(s) => {
        println(s"Got msg ${s}")
        sender ! StringMsg(s.reverse)
      }
    }
    
  }
 
  case class StringMsg(str : String)
}