package com.briatka.pavol.bookworm.customobjects;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "book", strict = false)
public class Book {

    @Element(name = "title")
    private String title;

    @Element(name = "average_rating")
    private String rating;

    @Element(name = "reviews_widget", data = true)
    private String reviews;

    public String getTitle() {
        return title;
    }

    public String getRating() {
        return rating;
    }

    public String getReviewsWidget() {
        return reviews;
    }
}
