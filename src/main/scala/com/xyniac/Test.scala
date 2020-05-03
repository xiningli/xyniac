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
    val jsonStr ="""{"name": "Mike", "age": 19}""".stripMargin
    val jsonStr2 ="""{"name": "Mike", "age": 20, "sex": "female"}""".stripMargin

    println(jsonStr)
    val personJson = parse(jsonStr)
    val personJson2 = parse(jsonStr2)
    val personJson3 = personJson2 merge  personJson // new merge old
    println("personJson3: " + personJson3)
    val person = personJson.extract[Person]
//    val person = gson.fromJson(jsonStr, classOf[Person])
    println(person)
    val jPerson = personJson.extract[JavaPerson]
    println(jPerson)

  }
}
