# Release all contract families as one versioned product

AnnoDocimal's published artifacts form one versioned product with four contract families: documentation capture and
carriers, transformation-author helpers, source projection, and Gradle integration. All artifacts release together so
cross-cutting compatibility changes—especially Java and Groovy baseline changes or major implementation-technology
switches—are coordinated and communicated as product-wide changes. Contract families may have different documented
stability levels, but modules do not evolve as independently versioned products.
