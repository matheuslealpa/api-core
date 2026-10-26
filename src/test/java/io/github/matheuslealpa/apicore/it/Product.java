package io.github.matheuslealpa.apicore.it;

import java.util.HashSet;
import java.util.Set;

import io.github.matheuslealpa.apicore.jpa.BaseEntity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    private String name;

    private int price;

    // Collection: blocked by the default RSQL whitelist, which only allows basic attributes.
    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private Set<String> tags = new HashSet<>();
}
