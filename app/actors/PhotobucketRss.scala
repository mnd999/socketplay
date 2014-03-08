package actors

import concurrent.duration._
import concurrent._
import spray.can.Http
import spray.http._
import spray.httpx.unmarshalling._
import spray.util._
import HttpMethods._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsArray
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scala.xml.NodeSeq
import scala.xml.XML
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import scala.io.Source
import java.io.FileOutputStream
import java.io.File
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import scala.xml.Node

class PhotobucketRecent extends Actor {

  private val photobucketurl = "http://feed.photobucket.com/recent/images/feed.rss"

  implicit val timeout = Timeout(5 seconds)

  import context._

  val fetcher = Akka.system.actorOf(Props[PhotoBucketFetcher])

  override def receive() = {
    case StringMsg(s) => {
      fetchUrl
      sender ! StringMsg("Done")
    }
  }

  def fetchUrl {

    val response: Future[HttpResponse] =
      (IO(Http) ? HttpRequest(GET, Uri(photobucketurl))).mapTo[HttpResponse]

    val xml = response.map(resp => resp.entity.as[NodeSeq] match {
      case Right(xml) => (xml \\ "item").foreach(photo => 
          fetcher ! new PhotoMsg((photo \ "creator").text, getPhotoUrl(photo \ "content"), (photo \ "link" ).text))
      case Left(error) => Unit
    })


   
    //println((Await.result(xml, 30 seconds)).attributes)

  }
  
  def getPhotoUrl(content : NodeSeq ): String = {
    content.head.attribute("url").map(x => x.text).getOrElse("")
  } 

}

class PhotoBucketFetcher extends Actor {

  import context._
  implicit val timeout = Timeout(10 seconds)
  
  val mongoDb = MongoClient("localhost", 27017)("photos")("photos")
  
  override def receive() = {
    case p : PhotoMsg => {
      println(p)
      saveImage(p)
  	}
    case StringMsg(s) => {
      println(s)
    }
  }

  def saveImage(photo : PhotoMsg) {
    
    val regex = ".*/(.*?)$".r    
    
    val filename = "/home/mark/images/" + (regex.findFirstIn(photo.url) match {
      case Some(regex(filename)) => {
        photo.user + "_media_" + filename
      }
      case None => "unknown"
    })
    
    println(filename)
    //println(url)
    
    if (!new File(filename).exists()) { 
	    val response: Future[HttpResponse] =
	      (IO(Http) ? HttpRequest(GET, Uri(photo.url))).mapTo[HttpResponse]
	    
	    val imagebytes = response.map(resp => resp.entity.data.toByteString)
	    
	    imagebytes.map(bytes => {
	      try {
	    	  val chan = new FileOutputStream(filename).getChannel()
	    	  chan.write(bytes.asByteBuffer)
	    	  chan.close()
	      }
	    })
	    saveMetaData(photo, filename)
    } else println("Already Got it!")
    
  }

  def saveMetaData(photo: PhotoMsg, filename: String) {
	  val doc = MongoDBObject("user" -> photo.user, "filename" -> filename, "url" -> photo.url, "webUrl" -> photo.webUrl)
      mongoDb.insert(doc)
  }
      
}

case class PhotoMsg(user: String, url: String, webUrl: String)
