package org.dazn.simpletvapp.data.repository

import org.dazn.simpletvapp.data.model.MediaItem
import org.dazn.simpletvapp.data.network.listDirectory

open class MediaRepository(
    private val networkFetcher: suspend (String) -> List<MediaItem> = { path ->
        org.dazn.simpletvapp.data.network.listDirectory(path)
    }
) {
    open suspend fun listDirectory(path: String): List<MediaItem> {
        val items = networkFetcher(path)
        return items
            .sortedWith(compareBy({ it.type != "directory" }, { it.name.lowercase() }))
    }
}
