# Support capture behavior without exposing its machinery

Java annotation processing, local Groovy AST capture, and global Groovy AST capture are supported behavioral integration
contracts that produce the documentation protocol. Consumers may rely on `@InlineJavadocs` driving configured Java APT
and local Groovy capture, and on the global-AST artifact enabling capture through service discovery. Processor,
transformation, visitor, source-parser, and Groovy-version-adapter classes are implementation types; required provider and
transformation class names remain packaging obligations rather than general-purpose source APIs.
