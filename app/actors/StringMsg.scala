package actors

case class StringMsg(str : String)

case class PhotoMsg(user: String, url: String, webUrl: String)