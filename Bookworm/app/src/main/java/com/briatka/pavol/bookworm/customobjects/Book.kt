package com.briatka.pavol.bookworm.customobjects

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "book", strict = false)
class Book @JvmOverloads constructor(
        @field:Element(name = "title")
        @param:Element(name = "title")
        val title: String? = null,
        @field:Element(name = "average_rating")
        @param:Element(name = "average_rating")
        val rating: String? = null,
        @field:Element(name = "reviews_widget", data = true)
        @param:Element(name = "reviews_widget", data = true)
        val reviewsWidget: String? = null

) {}
