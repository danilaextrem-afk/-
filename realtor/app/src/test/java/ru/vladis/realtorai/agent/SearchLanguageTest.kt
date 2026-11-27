package ru.vladis.realtorai.agent

import org.junit.Assert.*
import org.junit.Test

class SearchLanguageTest {
    @Test fun emptyCriteriaStartAnUnrestrictedApartmentSearch() {
        val turn = requireNotNull(SearchLanguage.resolve("""{"kind":"search","criteria":{}}""", SearchConversation()))
        assertEquals("search", turn.kind)
        assertEquals(Long.MAX_VALUE, turn.query.budget)
        assertNull(turn.query.rooms)
        assertEquals("", turn.query.place)
    }

    @Test fun followUpChangesOnlySuppliedCriteriaAndNullClearsBudget() {
        val earlier = SearchConversation(current = ListingQuery(place = "Химки", budget = 15_000_000, rooms = 2))
        val turn = requireNotNull(SearchLanguage.resolve(
            """{"kind":"refine","criteria":{"rooms":1,"budget":null}}""", earlier
        ))
        assertEquals("Химки", turn.query.place)
        assertEquals(1, turn.query.rooms)
        assertEquals(Long.MAX_VALUE, turn.query.budget)
        assertEquals(earlier.current, SearchLanguage.offline("Ещё", earlier)?.query)
    }
}
