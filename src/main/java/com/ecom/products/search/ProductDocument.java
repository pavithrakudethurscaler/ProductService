package com.ecom.products.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * Search copy of a product in Elasticsearch (HLD 3.2). The database stays the source of truth; this index only
 * answers "which product ids match", and results are loaded from the database.
 * createIndex = false: the index is created lazily by ProductSearchService, so the service still starts when
 * Elasticsearch is down.
 */
@Getter
@Setter
@Document(indexName = "products", createIndex = false)
public class ProductDocument {
    @Id
    private String id;
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "english"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String name;
    @Field(type = FieldType.Text, analyzer = "english")
    private String description;
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String categoryName;
    @Field(type = FieldType.Double)
    private double price;
}
