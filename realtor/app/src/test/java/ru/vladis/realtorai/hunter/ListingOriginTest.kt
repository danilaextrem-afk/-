package ru.vladis.realtorai.hunter

import org.junit.Assert.*
import org.junit.Test

class ListingOriginTest {
    @Test fun onlyDetailLinksExposeOriginalAction() {
        val row = Offer(source = "partner", url = "https://move.ru/objects/prodaetsya_1-komnatnaya_kvartira_ploschadyu_40_kvm_moskva_9297983784/")
        assertEquals("9297983784", ListingOrigin.original(row)?.id)
        assertEquals("9297983784", ListingOrigin.original(row.copy(url = "https://move.ru/objects/9297983784/"))?.id)
        assertNull(ListingOrigin.original(row.copy(url = "https://www.cian.ru/")))
        assertNull(ListingOrigin.original(row.copy(url = "https://www.cian.ru/kupit-kvartiru-bez-posrednikov/?id=12345678")))
        assertNull(ListingOrigin.original(row.copy(url = "https://example.com/search/12345678", externalId = "12345678")))
        assertNull(ListingOrigin.original(row.copy(url = "file:///storage/flat/12345678")))
    }

    @Test fun preservesRealPhotoReferencesAndRejectsMaskedPhone() {
        val row = HunterImport.fromJson("""
            [{"id":"10","seller":"owner","owner_phone":"+7 (999) 123-45-67","updated_at":"2026-11-25T10:15:00+03:00",
              "photos":["https://cdn.example.org/one.jpg",{"url":"https://cdn.example.org/two.webp"},"https://cdn.example.org/one.jpg","file:///tmp/x"]}]
        """.trimIndent(), "partner").single()
        assertEquals(2, ListingOrigin.photos(row.photoUrls).size)
        assertEquals("+79991234567", row.contactPhone)
        assertNotNull(row.sourceUpdatedAt)
        assertNull(row.ownerPhone)
        assertNull(ListingOrigin.dialable("+7 999 123-45-..."))
    }
}
