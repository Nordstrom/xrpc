package com.nordstrom.xrpc.demo.util

import com.nordstrom.xrpc.demo.model.Person
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi

object MoshiFactory {
  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  val personAdapter: JsonAdapter<Person> by lazy { moshi.adapter(Person::class.java) }

}
