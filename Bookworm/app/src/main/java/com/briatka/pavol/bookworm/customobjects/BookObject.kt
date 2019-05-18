package com.briatka.pavol.bookworm.customobjects

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root


@Root(name = "GoodreadsResponse", strict = false)
class BookObject  @JvmOverloads constructor(
        @field:Element(name = "book")
        @param:Element(name = "book")
        val book: Book? = null
) {}