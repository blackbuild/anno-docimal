# Support a transformation-author API

AnnoDocimal provides a first-class supported API for transformation authors to extract documentation from compiler model
elements, inspect it as a structured documentation model, build or rewrite it, apply templates, and attach it to generated
declarations. The supported contract is capability-oriented and does not automatically include source-position parsing,
Groovy-version adapters, visitors, metadata caches, or transformation machinery. A later allowlist may retain suitable
current helpers or introduce a curated facade without treating every existing public helper as stable.
