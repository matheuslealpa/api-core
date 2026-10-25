package com.github.matheuslealpa.apicore.it;

import java.util.UUID;

import com.github.matheuslealpa.apicore.jpa.BaseRepository;

interface ProductRepository extends BaseRepository<Product, UUID> {
}
