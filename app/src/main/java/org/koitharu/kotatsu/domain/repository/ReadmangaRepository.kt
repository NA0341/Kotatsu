package org.koitharu.kotatsu.domain.repository

import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.MangaRepository
import org.koitharu.kotatsu.domain.exceptions.ParseException
import org.koitharu.kotatsu.utils.ext.*

class ReadmangaRepository(loaderContext: MangaLoaderContext) : MangaRepository(loaderContext) {

    override suspend fun getList(
        offset: Int,
        query: String?,
        sortOrder: SortOrder?,
        tags: Set<String>?
    ): List<Manga> {
        val doc = loaderContext.get("https://readmanga.me/list?sortType=updated&offset=$offset")
            .parseHtml()
        val root = doc.body().getElementById("mangaBox")
            ?.selectFirst("div.tiles.row") ?: throw ParseException("Cannot find root")
        return root.select("div.tile").mapNotNull { node ->
            val imgDiv = node.selectFirst("div.img") ?: return@mapNotNull null
            val descDiv = node.selectFirst("div.desc") ?: return@mapNotNull null
            val href = imgDiv.selectFirst("a").attr("href")?.withDomain("readmanga.me")
                ?: return@mapNotNull null
            val title = descDiv.selectFirst("h3")?.selectFirst("a")?.text()
                ?: return@mapNotNull null
            Manga(
                id = href.longHashCode(),
                url = href,
                localizedTitle = title,
                title = descDiv.selectFirst("h4")?.text() ?: title,
                coverUrl = imgDiv.selectFirst("img.lazy")?.attr("data-original").orEmpty(),
                summary = "",
                rating = safe {
                    node.selectFirst("div.rating")
                        ?.attr("title")
                        ?.substringBefore(' ')
                        ?.toFloatOrNull()
                        ?.div(10f)
                } ?: -1f,
                tags = safe {
                    descDiv.selectFirst("div.tile-info")
                        ?.select("a.element-link")
                        ?.map {
                            MangaTag(
                                title = it.text(),
                                key = it.attr("href").substringAfterLast('/')
                            )
                        }?.toSet()
                }.orEmpty(),
                state = when {
                    node.selectFirst("div.tags")
                        ?.selectFirst("span.mangaCompleted") != null -> MangaState.FINISHED
                    else -> null
                },
                source = MangaSource.READMANGA_RU
            )
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = loaderContext.get(manga.url).parseHtml()
        val root = doc.body().getElementById("mangaBox")
        return manga.copy(
            description = root.selectFirst("div.manga-description").firstChild()?.html()?.parseAsHtml(),
            largeCoverUrl = root.selectFirst("div.subject-cower")?.selectFirst("img")?.attr(
                "data-full"
            ),
            chapters = root.selectFirst("div.chapters-link")?.selectFirst("table")
                ?.select("a")?.asReversed()?.mapIndexedNotNull { i, a ->
                val href =
                    a.attr("href")?.withDomain("readmanga.me") ?: return@mapIndexedNotNull null
                MangaChapter(
                    id = href.longHashCode(),
                    name = a.ownText(),
                    number = i + 1,
                    url = href,
                    source = MangaSource.READMANGA_RU
                )
            }
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}