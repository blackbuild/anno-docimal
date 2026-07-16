# Support Java 17 and one artifact set across Groovy 3, 4, and 5

AnnoDocimal's current compatibility contract is Java 17 minimum and one released artifact set supporting Groovy
generations 3, 4, and 5. Catalogued patch versions are tested baselines rather than the only permitted versions. Raising
the Java baseline or dropping a Groovy generation is a product-wide major compatibility change. If the Groovy 3 versus
4/5 JPMS module-name split proves unreasonable to solve across the related libraries, dropping Groovy 3 remains a
last-resort cross-project option; Jenkins no longer supplies the former Groovy 2.4 retention constraint. That option is
not selected by this session and must not be treated as the default resolution of issue #36 or #455.
