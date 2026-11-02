import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    @SerialName("end") val end: Int?,
    @SerialName("page") val page: Int?,
    @SerialName("per_page") val perPage: Int?,
    @SerialName("songs") val songs: List<Song?>?,
    @SerialName("start") val start: Int?,
    @SerialName("total_pages") val totalPages: Int?,
    @SerialName("total_songs") val totalSongs: Int?
)

@Serializable
data class Song(
    @SerialName("clip") val clip: Clip?,
)

@Serializable
data class Clip(
    @SerialName("allow_comments") val allowComments: Boolean?,
    @SerialName("audio_url") val audioUrl: String?,
    @SerialName("avatar_image_url") val avatarImageUrl: String?,
    @SerialName("caption") val caption: String?,
    @SerialName("comment_count") val commentCount: Int?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("display_name") val displayName: String?,
    @SerialName("display_tags") val displayTags: String?,
    @SerialName("handle") val handle: String?,
    @SerialName("id") val id: String?,
    @SerialName("image_large_url") val imageLargeUrl: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_handle_updated") val isHandleUpdated: Boolean?,
    @SerialName("is_liked") val isLiked: Boolean?,
    @SerialName("is_public") val isPublic: Boolean?,
    @SerialName("is_trashed") val isTrashed: Boolean?,
    @SerialName("major_model_version") val majorModelVersion: String?,
    @SerialName("metadata") val metadata: Metadata?,
    @SerialName("model_name") val modelName: String?,
    @SerialName("play_count") val playCount: Int?,
    @SerialName("reaction") val reaction: Reaction?,
    @SerialName("status") val status: String?,
    @SerialName("title") val title: String?,
    @SerialName("upvote_count") val upvoteCount: Int?,
    @SerialName("user_id") val userId: String?,
    @SerialName("video_cover_url") val videoCoverUrl: String?,
    @SerialName("video_url") val videoUrl: String?
)

@Serializable
data class Metadata(
    @SerialName("can_publish_with_vocal") val canPublishWithVocal: Boolean?,
    @SerialName("can_remix") val canRemix: Boolean?,
    @SerialName("cover_clip_id") val coverClipId: String?,
    @SerialName("duration") val duration: Double?,
    @SerialName("edited_clip_id") val editedClipId: String?,
    @SerialName("negative_tags") val negativeTags: String?,
    @SerialName("priority") val priority: Int?,
    @SerialName("prompt") val prompt: String?,
    @SerialName("refund_credits") val refundCredits: Boolean?,
    @SerialName("stream") val stream: Boolean?,
    @SerialName("tags") val tags: String?,
    @SerialName("type") val type: String?,
)

@Serializable
data class Reaction(
    @SerialName("play_count") val playCount: Int?,
    @SerialName("skip_count") val skipCount: Int?,
    @SerialName("updated_at") val updatedAt: String?
)
