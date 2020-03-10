/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

const val DEFAULT_MAXIMUM_DEPTH = 1
const val DEFAULT_MAXIMUM_URIS = 1_000_000
const val DEFAULT_INCLUDE_SUBDOMAINS = false
const val DEFAULT_CONCURRENT_REQUESTS = 1
const val DEFAULT_CONCURRENT_DOWNLOADS = 1

data class SpongeConfig(
    val spongeUri: SpongeUri,
    val mimeTypes: Set<String>,
    val fileExtensions: Set<String>,
    val maximumDepth: Int = DEFAULT_MAXIMUM_DEPTH,
    val maximumUris: Int = DEFAULT_MAXIMUM_URIS,
    val includeSubdomains: Boolean = DEFAULT_INCLUDE_SUBDOMAINS,
    val concurrentRequests: Int = DEFAULT_CONCURRENT_REQUESTS,
    val concurrentDownloads: Int = DEFAULT_CONCURRENT_DOWNLOADS
)
