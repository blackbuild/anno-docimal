# Store normalized textual documentation content

The textual documentation protocol stores normalized comment content without source delimiters such as `/** */` or
per-line decoration such as leading `*`. Capture adapters normalize language-specific documentation syntax; source
renderers add the syntax required by their output dialect. This keeps Java annotation processing and Groovy capture
equivalent and permits newer Java Markdown documentation comments to be supported by an adapter without changing the
carrier contract. Issue #30 owns future structured representation, not source-comment decoration.
