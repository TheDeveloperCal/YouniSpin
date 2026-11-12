package com.youni.younispin.metadata

object MetadataMatchResolver {
    private fun normalize(value: String?): String =
        value?.lowercase()?.replace(Regex("[^a-z0-9]+"), " ")?.trim() ?: ""

    fun chooseBestMatch(
        title: String,
        artist: String?,
        album: String?,
        durationMs: Long,
        candidates: List<MetadataMatch>
    ): MetadataMatch? {
        if (candidates.isEmpty()) return null

        val targetTitle = normalize(title)
        val targetArtist = normalize(artist)
        val targetAlbum = normalize(album)

        return candidates
            .map { candidate ->
                var score = candidate.score

                if (targetTitle.isNotEmpty()) {
                    val titleScore = if (normalize(candidate.title) == targetTitle) 35 else {
                        val candidateTitle = normalize(candidate.title)
                        when {
                            candidateTitle.isEmpty() -> 0
                            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 18
                            else -> 0
                        }
                    }
                    score += titleScore
                }

                if (targetArtist.isNotEmpty()) {
                    val artistScore = if (normalize(candidate.artist) == targetArtist) 30 else {
                        val candArtist = normalize(candidate.artist)
                        when {
                            candArtist.isEmpty() -> 0
                            candArtist.contains(targetArtist) || targetArtist.contains(candArtist) -> 12
                            else -> 0
                        }
                    }
                    score += artistScore
                }

                if (!album.isNullOrBlank()) {
                    val albumScore = if (normalize(candidate.album) == targetAlbum) 18 else {
                        val candAlbum = normalize(candidate.album)
                        when {
                            candAlbum.isEmpty() -> 0
                            candAlbum.contains(targetAlbum) || targetAlbum.contains(candAlbum) -> 7
                            else -> 0
                        }
                    }
                    score += albumScore
                }

                if (candidate.trackNumber != null) score += 8
                if (durationMs > 0L) score += 4

                when (candidate.confidence) {
                    MatchConfidence.STRONG_MATCH -> score += 20
                    MatchConfidence.POSSIBLE_MATCH -> score += 10
                    MatchConfidence.MULTIPLE_MATCHES -> score -= 10
                    MatchConfidence.LOW_MATCH -> score -= 15
                }

                candidate to score
            }
            .maxByOrNull { it.second }
            ?.first
            ?.takeIf { it.confidence != MatchConfidence.LOW_MATCH || it.score >= 85 }
    }

    fun canAutoApply(songTitle: String, songArtist: String, songAlbum: String, match: MetadataMatch): Boolean {
        val titleMatch = normalize(songTitle).isEmpty() || normalize(match.title) == normalize(songTitle)
        val artistMatch = normalize(songArtist).isEmpty() || normalize(match.artist) == normalize(songArtist)
        val albumMatch = normalize(songAlbum).isEmpty() || normalize(match.album) == normalize(songAlbum)

        if (match.confidence == MatchConfidence.STRONG_MATCH) return titleMatch && artistMatch && (albumMatch || normalize(match.album).isEmpty())
        if (match.confidence == MatchConfidence.POSSIBLE_MATCH) {
            return titleMatch && artistMatch
        }
        return false
    }
}
