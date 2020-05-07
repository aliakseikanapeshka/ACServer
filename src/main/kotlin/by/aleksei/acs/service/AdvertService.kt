package by.aleksei.acs.service

import by.aleksei.acs.Constants.EMPTY
import by.aleksei.acs.entities.AdItem
import by.aleksei.acs.entities.AdvertInfo
import by.aleksei.acs.entities.NewDetailsModel
import by.aleksei.acs.entities.UpdateDetailsModel
import by.aleksei.acs.entities.db.Advert
import by.aleksei.acs.entities.db.BookmarkId
import by.aleksei.acs.entities.db.Photo
import by.aleksei.acs.repository.AccountRepository
import by.aleksei.acs.repository.AdvertRepository
import by.aleksei.acs.repository.getAccountByToken
import by.aleksei.acs.util.ServiceResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

private const val PAGE_SIZE = 25

@Component
class AdvertService(
        private val accountRepository: AccountRepository,
        private val advertRepository: AdvertRepository) {

    fun add(token: String, advertInfo: NewDetailsModel): ServiceResponse<Boolean> {
        val account = accountRepository.getAccountByToken(token)
        val advert = Advert(
                title = advertInfo.title,
                price = advertInfo.price,
                userId = account?.id ?: -1,
                isShown = false,
                location = advertInfo.location,
                synopsis = advertInfo.synopsis,
                phone = advertInfo.phone,
                photos = advertInfo.photos.map { Photo(photo = it) }.toMutableList()
        )

        advertRepository.save(advert)

        return ServiceResponse.Success(true)
    }

    fun addRemoveToBookmark(token: String, bookmarkId: Int) = try {
        val account = accountRepository.getAccountByToken(token)
        val bookmark = BookmarkId(bookmarkId = bookmarkId)

        if (account?.bookmarkIds?.contains(bookmark) == true) {
            account.bookmarkIds.remove(bookmark)
        } else {
            account?.bookmarkIds?.add(bookmark)
        }

        account?.let { accountRepository.save(it) }

        true
    } catch (e: Exception) {
        false
    }

    fun showHideAd(token: String, advertId: Int) = try {
        val account = accountRepository.getAccountByToken(token)

        if (account?.isAdmin == true) {
            val advert = advertRepository.findByIdOrNull(advertId)
            val newAdvert = advert?.copy(isShown = !advert.isShown)
            newAdvert?.let { advertRepository.save(it) }

            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    fun update(token: String, advertInfo: UpdateDetailsModel): Boolean {
        try {
            val account = accountRepository.getAccountByToken(token)

            advertRepository.findByIdOrNull(advertInfo.id)?.let { advert ->
                if (advert.userId == account?.id) {
                    val updatedAd = advert.copy(
                            title = advertInfo.title,
                            price = advertInfo.price,
                            userId = account.id,
                            isShown = false,
                            location = advertInfo.location,
                            synopsis = advertInfo.synopsis,
                            phone = advertInfo.phone,
                            photos = advertInfo.photos.map { Photo(photo = it) }.toMutableList()

                    )

                    advertRepository.save(updatedAd)

                    return true
                }
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun delete(token: String, advertId: Int): Boolean {
        try {
            val account = accountRepository.getAccountByToken(token)

            advertRepository.findByIdOrNull(advertId)?.let {
                if (it.userId == account?.id) {
                    advertRepository.delete(it)
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun getLastAds(pageNumber: Int, isForceRefrech: Boolean = false): ServiceResponse<List<AdItem>> {
        val list = advertRepository.findAll()
                .sortedByDescending { it.date }
                .filter { it.isShown }
                .map {
                    AdItem(
                            id = it.id.toString(),
                            photoUrl = if (it.photos.isNotEmpty()) {
                                it.photos[0].photo
                            } else {
                                EMPTY
                            },
                            date = it.date,
                            title = it.title,
                            price = it.price,
                            place = it.location,
                            views = it.views
                    )
                }

        val resultList = if (isForceRefrech) {
            getAllPagesFromList(list, pageNumber)
        } else {
            getPageSublist(list, pageNumber)
        }

        return ServiceResponse.Success(resultList)
    }

    fun getMyAds(token: String, pageNumber: Int, isForceRefrech: Boolean = false): ServiceResponse<List<AdItem>> {
        val account = accountRepository.getAccountByToken(token)
        val list = advertRepository.findAll()
                .sortedByDescending { it.date }
                .filter { it.userId == account?.id ?: EMPTY }
                .map {
                    AdItem(
                            id = it.id.toString(),
                            photoUrl = if (it.photos.isNotEmpty()) {
                                it.photos[0].photo
                            } else {
                                EMPTY
                            },
                            date = it.date,
                            title = it.title,
                            price = it.price,
                            place = it.location,
                            views = it.views
                    )
                }

        val resultList = if (isForceRefrech) {
            getAllPagesFromList(list, pageNumber)
        } else {
            getPageSublist(list, pageNumber)
        }

        return ServiceResponse.Success(resultList)
    }

    fun getBookmarks(token: String, pageNumber: Int, isForceRefrech: Boolean = false): ServiceResponse<List<AdItem>> {
        val account = accountRepository.getAccountByToken(token)

        val userBookmarkIds = account?.bookmarkIds?.map { it.bookmarkId } ?: emptyList()

        val list = advertRepository.findAll()
                .sortedByDescending { it.date }
                .filter { userBookmarkIds.contains(it.id) && it.isShown }
                .map {
                    AdItem(
                            id = it.id.toString(),
                            photoUrl = if (it.photos.isNotEmpty()) {
                                it.photos[0].photo
                            } else {
                                EMPTY
                            },
                            date = it.date,
                            title = it.title,
                            price = it.price,
                            place = it.location,
                            views = it.views
                    )
                }

        val resultList = if (isForceRefrech) {
            getAllPagesFromList(list, pageNumber)
        } else {
            getPageSublist(list, pageNumber)
        }

        return ServiceResponse.Success(resultList)
    }

    fun getAdRequests(pageNumber: Int, isForceRefrech: Boolean = false): ServiceResponse<List<AdItem>> {
        val list = advertRepository.findAll()
                .sortedByDescending { it.date }
                .filter { !it.isShown }
                .map {
                    AdItem(
                            id = it.id.toString(),
                            photoUrl = if (it.photos.isNotEmpty()) {
                                it.photos[0].photo
                            } else {
                                EMPTY
                            },
                            date = it.date,
                            title = it.title,
                            price = it.price,
                            place = it.location,
                            views = it.views
                    )
                }

        val resultList = if (isForceRefrech) {
            getAllPagesFromList(list, pageNumber)
        } else {
            getPageSublist(list, pageNumber)
        }

        return ServiceResponse.Success(resultList)
    }

    fun getSearch(searchText: String, pageNumber: Int, sortType: Int, isForceRefrech: Boolean = false): ServiceResponse<List<AdItem>> {
        if (searchText.isEmpty()) {
            return ServiceResponse.Success(emptyList())
        }

        val words = searchText
                .split(Regex("\\s"))
                .filter { it.length > 1 }
                .map { it.toLowerCase() }

        val list = advertRepository.findAll()
                .sortedByDescending { it.date }
                .filter { advert ->
                    if (!advert.isShown) {
                        return@filter false
                    }

                    val title = advert.title.toLowerCase()
                    val synopsis = advert.synopsis.toLowerCase()

                    words.forEach { word ->
                        if (title.contains(word) || synopsis.contains(word)) {
                            return@filter true
                        }
                    }

                    return@filter false
                }
                .map {
                    AdItem(
                            id = it.id.toString(),
                            photoUrl = if (it.photos.isNotEmpty()) {
                                it.photos[0].photo
                            } else {
                                EMPTY
                            },
                            date = it.date,
                            title = it.title,
                            price = it.price,
                            place = it.location,
                            views = it.views
                    )
                }

        val resultList = if (isForceRefrech) {
            getAllPagesFromList(list, pageNumber)
        } else {
            getPageSublist(list, pageNumber)
        }

        return ServiceResponse.Success(resultList)
    }

    fun getDetails(token: String, detailsId: Int): ServiceResponse<AdvertInfo> {
        val details = advertRepository.findByIdOrNull(detailsId)
                ?: return ServiceResponse.Success(AdvertInfo(id = detailsId))
        val detailsAccount = accountRepository.findByIdOrNull(details.userId)
        val account = accountRepository.getAccountByToken(token)
        val accountBookmarks = account?.bookmarkIds ?: mutableSetOf()

        val updatedDetails = details.copy(
                views = details.views + 1
        )

        advertRepository.save(updatedDetails)

        val isShown = updatedDetails.isShown
        val isBookmark = accountBookmarks.map { it.bookmarkId }.contains(updatedDetails.id)

        val response = AdvertInfo(
                id = updatedDetails.id,
                photos = updatedDetails.photos.map { it.photo },
                title = updatedDetails.title,
                price = updatedDetails.price,
                location = updatedDetails.location,
                date = updatedDetails.date,
                views = updatedDetails.views,
                synopsis = updatedDetails.synopsis,
                username = detailsAccount?.username ?: EMPTY,
                phone = updatedDetails.phone,
                bookmark = isBookmark,
                showed = isShown
        )

        return ServiceResponse.Success(response)
    }

    private fun getPageSublist(list: List<AdItem>, pageNumber: Int): List<AdItem> {
        return when {
            list.size < pageNumber * PAGE_SIZE -> emptyList()
            list.size < pageNumber * PAGE_SIZE + PAGE_SIZE -> list.subList(pageNumber * PAGE_SIZE, list.size)
            else -> list.subList(pageNumber * PAGE_SIZE, pageNumber * PAGE_SIZE + PAGE_SIZE)
        }
    }

    private fun getAllPagesFromList(list: List<AdItem>, pageNumber: Int): List<AdItem> {
        return when {
            list.size < pageNumber * PAGE_SIZE + PAGE_SIZE -> list.subList(0, list.size)
            else -> list.subList(0, pageNumber * PAGE_SIZE + PAGE_SIZE)
        }
    }
}