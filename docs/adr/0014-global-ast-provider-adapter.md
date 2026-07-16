# Let the global-AST artifact own its service provider

`anno-docimal-global-ast` contains a minimal global transformation provider class and its service descriptor names that
local provider. The provider delegates to the reusable local transformation implementation in `anno-docimal-ast`. This
keeps global activation optional, preserves `@InlineJavadocs` local transformation use, and makes service-provider metadata
agree with module packaging. Issue #36 owns the implementation and classpath/module-path acceptance tests.
