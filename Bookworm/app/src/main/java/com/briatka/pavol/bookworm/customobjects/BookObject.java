package com.briatka.pavol.bookworm.customobjects;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root(name = "GoodreadsResponse", strict = false)
public class BookObject {


    @Element(name = "book")
    private Book book;


    public Book getBook() {
        return book;
    }

}





