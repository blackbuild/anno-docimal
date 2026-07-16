# Let the annotations artifact own the documentation protocol

`anno-docimal-annotations` is the stable cross-stage protocol artifact. It owns the compiled `@AnnoDoc` carrier, the
`@InlineJavadocs` capture marker, and externally observable documentation-properties naming and key semantics used across
capture, extraction, projection, and runtime consumption. This decision stabilizes protocol ownership, not the current
single-string encoding as the final model: issue #30 may add structured annotations for tags, format control, and
parameters. That future work must define compatibility and migration deliberately, but its annotation design is outside
this baseline session.
