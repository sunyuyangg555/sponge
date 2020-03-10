/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.commons.io.FileUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL
import java.nio.file.Path

private fun String.toSpongeUri() = SpongeUri(this)

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Path.of("testOutput").toAbsolutePath()
    private val fileName = "test.txt"
    private val imageFileName = "test.png"

    private val spongeConfig = SpongeConfig(
        "https://test.com".toSpongeUri(),
        setOf(ContentType.TEXT_PLAIN.mimeType),
        setOf("png")
    )

    private val spongeConfigWithSubdomains = spongeConfig.copy(includeSubdomains = true)
    private val spongeConfigWithDepthTwo = spongeConfig.copy(maximumDepth = 2)

    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteDirectory(outputDirectory.toFile())
    }

    @Test
    fun testEmptyDocument() {
        givenDocument(spongeConfig.spongeUri, "<html></html>")

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify(exactly = 0) { spongeService.download(any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(spongeConfig.spongeUri, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify(exactly = 0) { spongeService.download(any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri) }
    }

    @Test
    fun testDocumentWithLinkAndImage() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()
        val imageFileUri = "${spongeConfig.spongeUri}/$imageFileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                            <img src="$imageFileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)
        givenFile(imageFileUri, ContentType.IMAGE_PNG.mimeType)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri) }
        verify(exactly = 0) { spongeService.request(imageFileUri) }
        verify { spongeService.download(imageFileUri) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileUri = "https://www.test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
        verify(exactly = 0) { spongeService.download(fileUri) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithSubdomains.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeUri) }
        verify { spongeService.request(fileUri) }

        verify {
            spongeService.download(fileUri)
        }
    }

    @Test
    fun testDocumentWithIgnoredLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test2.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithSubdomains.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfigWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
        verify { spongeService.request(fileUri) }

        verify {
            spongeService.download(fileUri)
        }
    }

    @Test
    fun testDocumentWithChildDocumentAndDuplicateLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfigWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
        verify { spongeService.request(fileUri) }

        verify {
            spongeService.download(fileUri)
        }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToOneParent() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentUri,
            """
                    <html>
                        <body>
                           <a href="${spongeConfigWithDepthTwo.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToDirectParent() {
        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="${spongeConfig.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
    }

    @Test
    fun testDocumentWithTooDeepChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }

        verify(exactly = 0) {
            spongeService.download(fileUri)
        }
    }

    @Test
    fun testDocumentWithIgnoredChildDocument() {
        val childDocumentUri = "https://test2.com".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify(exactly = 0) { spongeService.request(childDocumentUri) }
    }

    @Test
    fun testDocumentWithInvalidChildUri() {
        val childUri = "http://"

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify { spongeService.request(any()) }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()
        val failingFileName = "test2.txt"
        val failingFileUri = "${spongeConfig.spongeUri}/$failingFileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$failingFileUri" />
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenUriFailsConnection(failingFileUri)

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri) }
        verify { spongeService.request(failingFileUri) }

        verify(exactly = 0) {
            spongeService.download(failingFileUri)
        }

        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri) }
    }

    @Test
    fun testDocumentWithLimitedVisitedLinks() {
        val otherConfig = spongeConfig.copy(maximumUris = 2)
        val fileUri = "${otherConfig.spongeUri}/$fileName".toSpongeUri()
        val otherFileUri = "${otherConfig.spongeUri}/test2.txt".toSpongeUri()

        givenDocument(
            otherConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                            <a href="$otherFileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)
        givenFile(otherFileUri)

        executeSponge(otherConfig)

        verify { spongeService.request(otherConfig.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri) }

        verify(exactly = 0) { spongeService.request(otherFileUri) }
        verify(exactly = 0) { spongeService.download(otherFileUri) }
    }

    private fun givenDocument(
        spongeUri: SpongeUri,
        htmlContent: String,
        contentType: ContentType = ContentType.TEXT_HTML
    ) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns URL(spongeUri.uri)

        every { spongeService.request(spongeUri) } returns response
    }

    private fun givenFile(spongeUri: SpongeUri, mimeType: String = ContentType.TEXT_PLAIN.mimeType) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns mimeType
        every { spongeService.request(spongeUri) } returns response
    }

    private fun givenUriFailsConnection(spongeUri: SpongeUri) {
        every { spongeService.request(spongeUri) } throws IOException("Error!")
    }

    private fun executeSponge(spongeConfig: SpongeConfig) {
        Sponge(spongeService, spongeConfig).execute()
    }
}
