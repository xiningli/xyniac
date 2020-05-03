package com.xyniac
//import com.google.gson.{Gson, JsonObject}
import com.google.gson.Gson
import org.json4s._
import org.json4s.native.JsonMethods._

object Test {
  implicit val formats = DefaultFormats
  def main(args: Array[String]): Unit = {
    val gson = new Gson()
    case class Person(name: String, age: Int)
    val jsonStrOld ="""{
                      |  "name": "Mike",
                      |  "city": "Stockholm"
                      |}""".stripMargin
    val jsonStrNew ="""{"sex": "female"}""".stripMargin

//    println(jsonStr)
    val jsonOld = parse(jsonStrOld)
//    personJson.children
    val jsonNew = parse(jsonStrNew)
    val personJson3 = jsonStrOld // new merge old
    val jsonResult = jsonOld merge jsonNew
    println("jsonResult: " + pretty(render(jsonResult)))
//    val person = personJson.extract[Person]
//    val person = gson.fromJson(jsonStr, classOf[Person])
//    println(person)
//    println(personJson)
//    val jPerson = personJson.extract[JavaPerson]
//    println(jPerson)

  }
}
