package com.briatka.pavol.bookworm.customobjects

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "book", strict = false)
class Book {

    @Element(name = "title")
    val title: String? = null

    @Element(name = "average_rating")
    val rating: String? = null

    @Element(name = "reviews_widget", data = true)
    val reviewsWidget: String? = null
}
