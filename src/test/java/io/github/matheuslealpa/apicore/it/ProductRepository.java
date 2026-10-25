package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import io.github.matheuslealpa.apicore.jpa.BaseRepository;

interface ProductRepository extends BaseRepository<Product, UUID> {
}
