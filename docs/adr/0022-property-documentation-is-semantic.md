# Treat Groovy property documentation as semantic documentation

Documentation written for a Groovy property is a library-owned semantic input. When capture runs with access to the
Groovy property model, it should retain the relationship between the property and its generated field/accessors so a
documented policy can project documentation to the appropriate declarations. Source projection must consume that
meaning rather than infer it unconditionally from JavaBeans-shaped method names. Issue #9 owns the detailed mapping,
including explicit accessor documentation and read-only or write-only properties; issue #30 may later provide a more
structured protocol representation.
