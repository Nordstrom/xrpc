package com.nordstrom.xrpc.demos.people

import com.nordstrom.xrpc.demo.extensions.addRoutable
import com.nordstrom.xrpc.demo.routes.PeopleRoutes
import com.nordstrom.xrpc.server.Server
import com.nordstrom.xrpc.testing.UnsafeHttp
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

@DisplayName("people-kotlin demo test")
internal class PeopleTests {
    private lateinit var server: Server
    private val client = UnsafeHttp.unsafeClient()

    @BeforeEach
    @Throws(IOException::class)
    fun beforeEach() {
        server = Server(0).apply {
            addRoutable(PeopleRoutes())
            listenAndServe()
        }
    }

    @AfterEach
    fun afterEach() {
        server.shutdown()
    }

    @Test
    @Throws(IOException::class)
    fun testPostPeople() {
        val response = client
                .newCall(
                        Request.Builder()
                                .post(RequestBody.create(MediaType.parse("application/json"), """{"name": "bob"}"""))
                                .url( "${server.localEndpoint()}/people")
                                .build())
                .execute()

        assertEquals(200, response.code())
    }

    @Test
    @Throws(IOException::class)
    fun testGetPeople() {
        client.newCall(
                Request.Builder()
                        .post(RequestBody.create(MediaType.parse("application/json"), """{"name": "bob"}"""))
                        .url("${server.localEndpoint()}/people")
                        .build())
                .execute()

        val response = client
                .newCall(Request.Builder().get().url(server.localEndpoint() + "/people").build())
                .execute()

        assertEquals("""[{"name":"bob"}]""", response.body()?.string())
    }

    @Test
    @Throws(IOException::class)
    fun testGetPerson() {
        client.newCall(
                Request.Builder()
                        .post(RequestBody.create(MediaType.parse("application/json"), """{"name": "bob"}"""))
                        .url("${server.localEndpoint()}/people")
                        .build()
        ).execute()

        val response = client.newCall(
                Request.Builder().get().url(server.localEndpoint() + "/people/bob").build()
        ).execute()

        assertEquals("""{"name":"bob"}""", response.body()?.string())
    }

    @Test
    @Throws(IOException::class)
    fun testGetPersonNotFound() {
        val response = client.newCall(
                Request.Builder().get().url("${server.localEndpoint()}/people/bob").build()
        ).execute()

        assertEquals(404, response.code())
    }
}
