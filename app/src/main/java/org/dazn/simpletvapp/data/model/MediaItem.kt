package org.dazn.simpletvapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FfprobeStream(
    val codec_name: String? = null,
    val codec_type: String? = null,
    val profile: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sample_aspect_ratio: String? = null,
    val display_aspect_ratio: String? = null,
)

@Serializable
data class FfprobeFormat(
    val format_name: String? = null,
)

@Serializable
data class FfprobeResponse(
    val streams: List<FfprobeStream> = emptyList(),
    val format: FfprobeFormat? = null,
)

@Serializable
data class MediaItem(
    val path: String,
    val type: String,
    val size: Long? = null,
    @SerialName("content_type") val contentType: String? = null,
    val streamFormat: String? = null,
    val ffprobe_response: FfprobeResponse? = null,
) {
    val name: String get() = path.substringAfterLast("/").ifEmpty { path }
    val isPhoto: Boolean get() = contentType?.startsWith("image/") == true
}
