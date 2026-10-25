package com.github.matheuslealpa.apicore.it;

import com.github.matheuslealpa.apicore.jpa.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Product extends BaseEntity {

    private String name;

    protected Product() {
    }

    Product(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
