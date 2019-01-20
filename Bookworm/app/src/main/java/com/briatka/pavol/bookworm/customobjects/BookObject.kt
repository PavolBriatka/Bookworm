package com.briatka.pavol.bookworm.customobjects

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root


@Root(name = "GoodreadsResponse", strict = false)
class BookObject {


    @Element(name = "book")
    val book: Book? = null

}