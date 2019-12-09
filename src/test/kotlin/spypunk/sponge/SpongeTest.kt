/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val spongeInput = SpongeInput(
            URI("https://www.test.com"),
            File("output"),
            setOf(ContentType.TEXT_PLAIN.mimeType)
    )

    @Test
    fun testEmptyDocument() {
        val htmlContent = "<html></html>"

        every { spongeService.connect(spongeInput.uri) } returns
                response(htmlContent, spongeInput.uri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithTextLink() {
        val fileName = "test.txt"

        val htmlContent =
                """
                    <html>
                        <body>
                            <a href="/$fileName" />
                        </body>
                    </html>
                """

        every { spongeService.connect(spongeInput.uri) } returns
                response(htmlContent, spongeInput.uri)

        val fileUri = URI("${spongeInput.uri}/$fileName")

        every { spongeService.connect(fileUri) } returns
                response(ContentType.TEXT_PLAIN.mimeType)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri) }
        verify(exactly = 1) { spongeService.connect(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, File(spongeInput.outputDirectory, fileName)) }
    }

    private fun response(htmlContent: String, baseUri: URI): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_HTML.mimeType
        every { response.parse() } returns Jsoup.parse(htmlContent, baseUri.toString())

        return response
    }

    private fun response(contentType: String): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType

        return response
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
