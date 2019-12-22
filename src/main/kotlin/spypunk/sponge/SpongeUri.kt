/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.google.common.escape.Escaper
import com.google.common.net.UrlEscapers
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import java.net.URI
import java.net.URL

private const val WWW_PREFIX = "www."

private val supportedSchemes = setOf("http", "https")
private val urlPathSegmentEscaper: Escaper = UrlEscapers.urlPathSegmentEscaper()

class SpongeUri private constructor(private val uri: String) {
    companion object {
        operator fun invoke(uri: String): SpongeUri {
            val url = URL(uri)

            if (!supportedSchemes.contains(url.protocol)) error("Unsupported scheme: ${url.protocol}")
            if (url.host.isNullOrEmpty()) error("Hostname cannot be empty")

            val normalizedUri = URIBuilder()
                .apply {
                    scheme = url.protocol
                    port = url.port
                    userInfo = url.userInfo

                    host = if (url.host.startsWith(WWW_PREFIX)) {
                        url.host.substring(WWW_PREFIX.length)
                    } else {
                        url.host
                    }

                    if (!url.query.isNullOrEmpty()) {
                        setCustomQuery(url.query)
                    }

                    if (!url.path.isNullOrEmpty()) {
                        pathSegments = URLEncodedUtils.parsePathSegments(url.path).map(urlPathSegmentEscaper::escape)
                    }
                }
                .build()
                .normalize()

            return SpongeUri(normalizedUri.toString())
        }
    }

    override fun toString() = uri

    override fun equals(other: Any?): Boolean {
        return if (other is SpongeUri) {
            uri == other.uri
        } else {
            false
        }
    }

    override fun hashCode() = uri.hashCode()

    fun toUri() = URI(uri)
}

fun String.toSpongeUri() = SpongeUri(this)