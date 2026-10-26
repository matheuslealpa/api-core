package io.github.matheuslealpa.apicore.it;

import io.github.matheuslealpa.apicore.jpa.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Product extends BaseEntity {

    private String name;

    private int price;

    protected Product() {
    }

    Product(String name) {
        this(name, 0);
    }

    Product(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }
}
